package com.allocra.common.error;

/**
 * Thrown when a booking lifecycle transition is not allowed from the booking's
 * current state (e.g. cancelling an already-cancelled booking). The web layer
 * maps this to HTTP 409.
 */
public class InvalidBookingStateException extends RuntimeException {
	public InvalidBookingStateException(String message) {
		super(message);
	}
}
