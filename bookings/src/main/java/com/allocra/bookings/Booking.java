package com.allocra.bookings;

import com.allocra.common.tenant.TenantId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A commitment to provide a configured service at a given time for a subject
 * (ADR-003, PRD-BKG-001). It holds <strong>no</strong> resource ids — the
 * resources satisfying it are recorded as separate {@link ResourceAssignment}s.
 *
 * @param tenantId
 *            owning tenant
 * @param id
 *            booking id
 * @param serviceTypeId
 *            the service booked
 * @param subject
 *            who/what receives the service
 * @param startAt
 *            slot start (UTC)
 * @param endAt
 *            slot end (UTC)
 * @param status
 *            lifecycle status
 * @param channel
 *            creation channel
 */
public record Booking(TenantId tenantId, UUID id, UUID serviceTypeId, BookingSubject subject, Instant startAt,
		Instant endAt, BookingStatus status, BookingChannel channel) {

	public Booking {
		Objects.requireNonNull(tenantId, "tenantId");
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(serviceTypeId, "serviceTypeId");
		Objects.requireNonNull(subject, "subject");
		Objects.requireNonNull(startAt, "startAt");
		Objects.requireNonNull(endAt, "endAt");
		Objects.requireNonNull(status, "status");
		Objects.requireNonNull(channel, "channel");
		if (!endAt.isAfter(startAt)) {
			throw new IllegalArgumentException("endAt must be after startAt");
		}
	}
}
