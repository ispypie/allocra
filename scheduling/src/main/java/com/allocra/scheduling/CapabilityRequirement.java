package com.allocra.scheduling;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A capability a service requires of a resource (PRD-SVC-002/006): a required
 * type, an optional minimum level, and validity checked on the appointment
 * date.
 *
 * @param type
 *            required capability type; never {@code null}
 * @param minLevel
 *            optional minimum level; {@code null} means any level (or none) is
 *            acceptable
 */
public record CapabilityRequirement(String type, Integer minLevel) {

	public CapabilityRequirement {
		Objects.requireNonNull(type, "type");
	}

	public static CapabilityRequirement of(String type) {
		return new CapabilityRequirement(type, null);
	}

	/**
	 * True if {@code capability} satisfies this requirement on
	 * {@code appointmentDate}.
	 */
	public boolean satisfiedBy(CapabilitySpec capability, LocalDate appointmentDate) {
		if (!type.equals(capability.type())) {
			return false;
		}
		if (!capability.validOn(appointmentDate)) {
			return false;
		}
		if (minLevel == null) {
			return true;
		}
		return capability.level() != null && capability.level() >= minLevel;
	}
}
