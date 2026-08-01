package com.allocra.reservations;

/**
 * Reservation states. Only ACTIVE reservations participate in the overlap
 * exclusion (ADR-004).
 */
public enum ReservationStatus {
	ACTIVE, RELEASED
}
