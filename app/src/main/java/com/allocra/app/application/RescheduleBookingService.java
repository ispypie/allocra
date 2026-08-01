package com.allocra.app.application;

import com.allocra.app.persistence.BookingReadRepository;
import com.allocra.app.persistence.BookingWriteRepository;
import com.allocra.app.persistence.CatalogRepository;
import com.allocra.app.persistence.CatalogRepository.ServiceDefinition;
import com.allocra.bookings.BookingStatus;
import com.allocra.common.error.InfeasibleBookingException;
import com.allocra.common.error.InvalidBookingStateException;
import com.allocra.common.error.NotAuthorizedException;
import com.allocra.common.error.ResourceNotFoundException;
import com.allocra.common.tenant.TenantId;
import com.allocra.membership.Membership;
import com.allocra.membership.Permission;
import com.allocra.reservations.Reservation;
import com.allocra.reservations.ReservationStatus;
import com.allocra.scheduling.DirectAvailabilitySearch;
import com.allocra.scheduling.Interval;
import com.allocra.scheduling.SchedulingSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reschedules a CONFIRMED booking to a new time (PRD-BKG-012). The booking
 * keeps its identity and its assignments (same resources); only the time and
 * reservations move. Within one transaction the old reservations are released,
 * feasibility at the new time is re-validated against current availability, and
 * new reservations are created — a conflict at the new time rolls the whole
 * thing back, leaving the booking unchanged (PRD-RSV-004, ADR-003).
 */
@Service
public class RescheduleBookingService {

	private final CatalogRepository catalog;
	private final BookingReadRepository reads;
	private final BookingWriteRepository writes;
	private final SchedulingSnapshotBuilder snapshots;
	private final DirectAvailabilitySearch engine = new DirectAvailabilitySearch();

	public RescheduleBookingService(CatalogRepository catalog, BookingReadRepository reads,
			BookingWriteRepository writes, SchedulingSnapshotBuilder snapshots) {
		this.catalog = catalog;
		this.reads = reads;
		this.writes = writes;
		this.snapshots = snapshots;
	}

	@Transactional
	public void reschedule(TenantId tenantId, Membership membership, UUID bookingId, Instant newStart) {
		if (!membership.has(Permission.BOOKING_RESCHEDULE)) {
			throw new NotAuthorizedException("missing permission BOOKING_RESCHEDULE");
		}
		var booking = reads.findById(tenantId, bookingId)
				.orElseThrow(() -> new ResourceNotFoundException("booking " + bookingId + " not found"));
		if (!BookingStatus.CONFIRMED.name().equals(booking.status())) {
			throw new InvalidBookingStateException("booking " + bookingId + " is " + booking.status()
					+ "; only CONFIRMED bookings can be rescheduled");
		}

		ServiceDefinition service = catalog.findService(tenantId, booking.serviceTypeId())
				.orElseThrow(() -> new ResourceNotFoundException("service " + booking.serviceTypeId() + " not found"));
		Interval newSlot = new Interval(newStart, newStart.plus(Duration.ofMinutes(service.durationMinutes())));
		Map<UUID, UUID> chosen = reads.findAssignmentMap(tenantId, bookingId);

		// Release the booking's own reservations first so the new-time re-check does
		// not treat
		// them as blocking (they may overlap the new slot). A rollback restores them.
		writes.releaseReservations(tenantId, bookingId);

		SchedulingSnapshot snapshot = snapshots.forConfirm(tenantId, service, newSlot, chosen);
		if (engine.search(snapshot).isEmpty()) {
			throw new InfeasibleBookingException("the booking's resources are not available at the requested new time");
		}

		for (UUID resourceId : chosen.values()) {
			writes.insertReservation(new Reservation(tenantId, UUID.randomUUID(), bookingId, resourceId,
					newSlot.start(), newSlot.end(), ReservationStatus.ACTIVE));
		}
		writes.updateSlot(tenantId, bookingId, newSlot.start(), newSlot.end());
		writes.insertAudit(tenantId, membership.userId().toString(), "BOOKING_RESCHEDULED", "booking", bookingId);
	}
}
