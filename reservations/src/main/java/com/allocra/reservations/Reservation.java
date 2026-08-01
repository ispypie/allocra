package com.allocra.reservations;

import com.allocra.common.tenant.TenantId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * An exclusive time-range consumption of a resource, created when a booking is
 * confirmed (PRD-RSV-001/003). Overlap is prevented by a PostgreSQL exclusion
 * constraint (ADR-004).
 *
 * @param tenantId
 *            owning tenant
 * @param id
 *            reservation id
 * @param bookingId
 *            the booking that created it
 * @param resourceId
 *            the consumed resource
 * @param startAt
 *            window start (UTC)
 * @param endAt
 *            window end (UTC)
 * @param status
 *            ACTIVE or RELEASED
 */
public record Reservation(TenantId tenantId, UUID id, UUID bookingId, UUID resourceId, Instant startAt, Instant endAt,
		ReservationStatus status) {

	public Reservation {
		Objects.requireNonNull(tenantId, "tenantId");
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(bookingId, "bookingId");
		Objects.requireNonNull(resourceId, "resourceId");
		Objects.requireNonNull(startAt, "startAt");
		Objects.requireNonNull(endAt, "endAt");
		Objects.requireNonNull(status, "status");
	}
}
