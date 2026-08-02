package com.allocra.app.application;

import com.allocra.app.persistence.BookingWriteRepository;
import com.allocra.app.persistence.CatalogRepository;
import com.allocra.app.persistence.CatalogRepository.RequirementRow;
import com.allocra.app.persistence.CatalogRepository.ServiceDefinition;
import com.allocra.bookings.AssignmentPolicy;
import com.allocra.bookings.Booking;
import com.allocra.bookings.BookingChannel;
import com.allocra.bookings.BookingStatus;
import com.allocra.bookings.BookingSubject;
import com.allocra.bookings.ResourceAssignment;
import com.allocra.common.error.InfeasibleBookingException;
import com.allocra.common.error.NotAuthorizedException;
import com.allocra.common.error.ResourceNotFoundException;
import com.allocra.common.tenant.TenantId;
import com.allocra.membership.Membership;
import com.allocra.membership.Permission;
import com.allocra.reservations.Reservation;
import com.allocra.reservations.ReservationStatus;
import com.allocra.scheduling.AssignmentChoice;
import com.allocra.scheduling.CandidateOption;
import com.allocra.scheduling.DirectAvailabilitySearch;
import com.allocra.scheduling.Interval;
import com.allocra.scheduling.ResourceCandidate;
import com.allocra.scheduling.SchedulingSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confirms a booking atomically (PRD-BKG-004/005/006, ADR-003, ADR-004). Within
 * a single transaction it re-validates feasibility, then inserts the booking,
 * one assignment per satisfied requirement and one reservation per exclusive
 * assigned resource — or fails without creating a partial booking. A concurrent
 * conflict surfaces as
 * {@link com.allocra.common.error.ReservationConflictException} → HTTP 409
 * (PRD-RSV-004).
 */
@Service
public class ConfirmBookingService {

	private final CatalogRepository catalog;
	private final SchedulingSnapshotBuilder snapshots;
	private final BookingWriteRepository writes;
	private final DirectAvailabilitySearch engine = new DirectAvailabilitySearch();

	public ConfirmBookingService(CatalogRepository catalog, SchedulingSnapshotBuilder snapshots,
			BookingWriteRepository writes) {
		this.catalog = catalog;
		this.snapshots = snapshots;
		this.writes = writes;
	}

	@Transactional
	public UUID confirm(TenantId tenantId, Membership membership, ConfirmCommand command) {
		if (!membership.has(Permission.BOOKING_CREATE)) {
			throw new NotAuthorizedException("missing permission BOOKING_CREATE");
		}
		ServiceDefinition service = catalog.findService(tenantId, command.serviceTypeId())
				.orElseThrow(() -> new ResourceNotFoundException("service " + command.serviceTypeId() + " not found"));

		Instant end = command.start().plus(Duration.ofMinutes(service.durationMinutes()));
		Interval slot = new Interval(command.start(), end);

		// Every hard requirement must have a chosen resource before we even look at the
		// DB
		// (feasibility-before-confirmation, PRD-BKG-005).
		for (RequirementRow row : service.requirements()) {
			if (row.required() && !command.assignments().containsKey(row.id())) {
				throw new InfeasibleBookingException("required requirement " + row.id() + " has no assignment");
			}
		}

		// Re-validate the exact chosen combination against current availability
		// (PRD-BKG-006).
		SchedulingSnapshot snapshot = snapshots.forConfirm(tenantId, service, slot, command.assignments());
		List<CandidateOption> options = engine.search(snapshot);
		if (options.isEmpty()) {
			throw new InfeasibleBookingException("the chosen resources are not available for the requested time");
		}
		CandidateOption option = options.get(0);

		UUID bookingId = UUID.randomUUID();
		writes.insertBooking(new Booking(tenantId, bookingId, service.serviceTypeId(), command.subject(), slot.start(),
				slot.end(), BookingStatus.CONFIRMED, BookingChannel.INTERNAL));

		for (AssignmentChoice choice : option.assignments()) {
			UUID requirementId = UUID.fromString(choice.requirementId());
			UUID resourceId = UUID.fromString(choice.resourceId());
			writes.insertAssignment(tenantId, new ResourceAssignment(UUID.randomUUID(), bookingId, requirementId,
					resourceId, AssignmentPolicy.REASSIGNABLE));
			// Every assigned resource is exclusive → one reservation each (PRD-RSV-003),
			// over the
			// buffered window (lead + setup before, cleanup after) so turnover is
			// protected.
			Interval reserved = reservedWindow(snapshot, choice, slot);
			writes.insertReservation(new Reservation(tenantId, UUID.randomUUID(), bookingId, resourceId,
					reserved.start(), reserved.end(), ReservationStatus.ACTIVE));
		}

		writes.insertAudit(tenantId, membership.userId().toString(), "BOOKING_CONFIRMED", "booking", bookingId);
		return bookingId;
	}

	private static Interval reservedWindow(SchedulingSnapshot snapshot, AssignmentChoice choice, Interval slot) {
		ResourceCandidate candidate = snapshot.candidateFor(choice.requirementId(), choice.resourceId());
		return candidate == null ? slot : candidate.reservedWindow(slot, snapshot.leadMinutes());
	}

	/**
	 * @param serviceTypeId
	 *            the service to book
	 * @param start
	 *            the chosen slot start (UTC)
	 * @param subject
	 *            who/what receives the service
	 * @param assignments
	 *            chosen resource per requirement (requirementId → resourceId)
	 */
	public record ConfirmCommand(UUID serviceTypeId, Instant start, BookingSubject subject,
			Map<UUID, UUID> assignments) {
		public ConfirmCommand {
			assignments = Map.copyOf(assignments);
		}
	}
}
