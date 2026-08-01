package com.allocra.bookings;

import java.util.Objects;

/**
 * The person, organisation or asset receiving the service (PRD-SUB-*).
 * Deliberately neutral — not a {@code Customer}, not a CRM record. Only the
 * minimal contact fields are modelled.
 *
 * @param type
 *            subject type discriminator (e.g. "PERSON", "ORGANISATION",
 *            "ASSET")
 * @param displayName
 *            display name; never {@code null}
 * @param email
 *            optional email
 * @param phone
 *            optional phone
 * @param externalRef
 *            optional external reference into another system
 */
public record BookingSubject(String type, String displayName, String email, String phone, String externalRef) {

	public BookingSubject {
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(displayName, "displayName");
	}
}
