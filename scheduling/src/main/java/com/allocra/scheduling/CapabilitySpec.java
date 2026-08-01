package com.allocra.scheduling;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A capability a resource possesses (PRD-RES-004): a typed marker with an
 * optional level and an optional validity window (e.g. a qualification with a
 * certification expiry — PRD-SVC-006).
 *
 * @param type
 *            capability type code (e.g. "PHYSIOTHERAPY_LICENCE"); never
 *            {@code null}
 * @param level
 *            optional numeric level/grade; {@code null} if not levelled
 * @param validFrom
 *            optional inclusive first valid date; {@code null} means no lower
 *            bound
 * @param validTo
 *            optional inclusive last valid date; {@code null} means no expiry
 */
public record CapabilitySpec(String type, Integer level, LocalDate validFrom, LocalDate validTo) {

	public CapabilitySpec {
		Objects.requireNonNull(type, "type");
	}

	/** Convenience for a non-levelled, never-expiring capability. */
	public static CapabilitySpec of(String type) {
		return new CapabilitySpec(type, null, null, null);
	}

	/**
	 * True if this capability is valid on the given date (PRD-SVC-006 validity).
	 */
	public boolean validOn(LocalDate date) {
		Objects.requireNonNull(date, "date");
		boolean afterStart = validFrom == null || !date.isBefore(validFrom);
		boolean beforeEnd = validTo == null || !date.isAfter(validTo);
		return afterStart && beforeEnd;
	}
}
