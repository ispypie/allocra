package com.allocra.app.application;

import com.allocra.app.persistence.BookingReadRepository;
import com.allocra.app.persistence.BookingReadRepository.BookingSummary;
import com.allocra.app.persistence.BookingReadRepository.BookingView;
import com.allocra.common.error.NotAuthorizedException;
import com.allocra.common.error.ResourceNotFoundException;
import com.allocra.common.tenant.TenantId;
import com.allocra.membership.Membership;
import com.allocra.membership.Permission;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Read-side booking queries, permission-checked and tenant-scoped
 * (PRD-BKG-010/011).
 */
@Service
public class BookingQueryService {

	private final BookingReadRepository bookings;

	public BookingQueryService(BookingReadRepository bookings) {
		this.bookings = bookings;
	}

	public BookingView get(TenantId tenantId, Membership membership, UUID bookingId) {
		requireView(membership);
		return bookings.findById(tenantId, bookingId)
				.orElseThrow(() -> new ResourceNotFoundException("booking " + bookingId + " not found"));
	}

	public List<BookingSummary> list(TenantId tenantId, Membership membership, Optional<String> status) {
		requireView(membership);
		return bookings.list(tenantId, status);
	}

	private static void requireView(Membership membership) {
		if (!membership.has(Permission.BOOKING_VIEW)) {
			throw new NotAuthorizedException("missing permission BOOKING_VIEW");
		}
	}
}
