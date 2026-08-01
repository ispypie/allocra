package com.allocra.scheduling;

import java.util.Objects;

/**
 * A proposed binding of one resource to one requirement within a candidate
 * option. This is the engine's advisory output; it becomes a persisted
 * {@code ResourceAssignment} only when a booking is confirmed (ADR-003). The
 * engine never mutates existing assignments (PRD-ASN-004).
 *
 * @param requirementId
 *            the requirement being satisfied
 * @param resourceId
 *            the resource selected to satisfy it
 */
public record AssignmentChoice(String requirementId, String resourceId) {

	public AssignmentChoice {
		Objects.requireNonNull(requirementId, "requirementId");
		Objects.requireNonNull(resourceId, "resourceId");
	}
}
