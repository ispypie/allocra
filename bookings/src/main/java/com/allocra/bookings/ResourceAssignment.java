package com.allocra.bookings;

import java.util.Objects;
import java.util.UUID;

/**
 * The resource currently selected to satisfy one requirement of a booking
 * (ADR-003, PRD-ASN-001). It is a separate record from the booking, so it can
 * change without changing the booking's identity (PRD-ASN-002).
 *
 * @param id
 *            assignment id
 * @param bookingId
 *            the booking this assignment belongs to
 * @param requirementId
 *            the requirement it satisfies
 * @param resourceId
 *            the selected resource
 * @param policy
 *            the assignment policy
 */
public record ResourceAssignment(UUID id, UUID bookingId, UUID requirementId, UUID resourceId,
		AssignmentPolicy policy) {

	public ResourceAssignment {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(bookingId, "bookingId");
		Objects.requireNonNull(requirementId, "requirementId");
		Objects.requireNonNull(resourceId, "resourceId");
		Objects.requireNonNull(policy, "policy");
	}
}
