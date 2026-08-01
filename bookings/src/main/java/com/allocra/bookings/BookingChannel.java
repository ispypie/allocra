package com.allocra.bookings;

/**
 * The channel a booking was created through (PRD-BKG-003). Only
 * {@link #INTERNAL} is delivered initially, but every booking records its
 * channel so the future self-service channel is additive, not a redesign
 * (ADR-005).
 */
public enum BookingChannel {
	INTERNAL, SELF_SERVICE, API, IMPORT
}
