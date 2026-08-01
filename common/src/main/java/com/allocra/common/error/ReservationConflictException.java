package com.allocra.common.error;

/**
 * Thrown when a resource could not be reserved because it became unavailable —
 * typically a concurrent confirmation winning the race enforced by the
 * PostgreSQL exclusion constraint (ADR-004). The web layer maps this to an HTTP
 * 409 Conflict (PRD-BKG-006 / PRD-RSV-004).
 *
 * <p>
 * Availability search is advisory; a slot returned by search may be gone by
 * confirmation time (PRD-RSV-005). This exception is the signal for that case.
 */
public class ReservationConflictException extends RuntimeException {

	public ReservationConflictException(String message) {
		super(message);
	}

	public ReservationConflictException(String message, Throwable cause) {
		super(message, cause);
	}
}
