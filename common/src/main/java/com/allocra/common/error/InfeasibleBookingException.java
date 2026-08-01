package com.allocra.common.error;

/**
 * Thrown when a booking cannot be confirmed because a hard requirement has no
 * valid assignment at the requested time (PRD-BKG-005) — distinct from a
 * reservation conflict. The web layer maps this to HTTP 422.
 */
public class InfeasibleBookingException extends RuntimeException {
	public InfeasibleBookingException(String message) {
		super(message);
	}
}
