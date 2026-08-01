package com.allocra.app.application;

import com.allocra.app.persistence.BookingWriteRepository;
import com.allocra.bookings.BookingStatus;
import com.allocra.common.error.InvalidBookingStateException;
import com.allocra.common.error.NotAuthorizedException;
import com.allocra.common.error.ResourceNotFoundException;
import com.allocra.common.tenant.TenantId;
import com.allocra.membership.Membership;
import com.allocra.membership.Permission;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Booking lifecycle transitions (PRD-BKG-008/009). Each transition is
 * permission-checked, only valid from {@code CONFIRMED}, and transactional.
 * Cancellation additionally releases the booking's reservations so the freed
 * slot becomes bookable again (PRD-BKG-008).
 */
@Service
public class BookingLifecycleService {

	private final BookingWriteRepository bookings;

	public BookingLifecycleService(BookingWriteRepository bookings) {
		this.bookings = bookings;
	}

	@Transactional
	public void cancel(TenantId tenantId, Membership membership, UUID bookingId) {
		transition(tenantId, membership, bookingId, Permission.BOOKING_CANCEL, BookingStatus.CANCELLED);
		bookings.releaseReservations(tenantId, bookingId);
		bookings.insertAudit(tenantId, membership.userId().toString(), "BOOKING_CANCELLED", "booking", bookingId);
	}

	@Transactional
	public void complete(TenantId tenantId, Membership membership, UUID bookingId) {
		transition(tenantId, membership, bookingId, Permission.BOOKING_UPDATE, BookingStatus.COMPLETED);
		bookings.insertAudit(tenantId, membership.userId().toString(), "BOOKING_COMPLETED", "booking", bookingId);
	}

	@Transactional
	public void noShow(TenantId tenantId, Membership membership, UUID bookingId) {
		transition(tenantId, membership, bookingId, Permission.BOOKING_UPDATE, BookingStatus.NO_SHOW);
		bookings.insertAudit(tenantId, membership.userId().toString(), "BOOKING_NO_SHOW", "booking", bookingId);
	}

	private void transition(TenantId tenantId, Membership membership, UUID bookingId, Permission permission,
			BookingStatus target) {
		if (!membership.has(permission)) {
			throw new NotAuthorizedException("missing permission " + permission);
		}
		String current = bookings.findStatus(tenantId, bookingId)
				.orElseThrow(() -> new ResourceNotFoundException("booking " + bookingId + " not found"));
		if (!BookingStatus.CONFIRMED.name().equals(current)) {
			throw new InvalidBookingStateException("booking " + bookingId + " is " + current
					+ "; only CONFIRMED bookings can transition to " + target);
		}
		int updated = bookings.updateStatus(tenantId, bookingId, BookingStatus.CONFIRMED.name(), target.name());
		if (updated == 0) {
			// Lost a race: another transition changed the status between our read and
			// write.
			throw new InvalidBookingStateException("booking " + bookingId + " was concurrently modified");
		}
	}
}
