package com.allocra.scheduling;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The immutable input to the scheduling engine (PRD-SCH-001). It carries
 * everything needed to compute candidate options — the request window and
 * service duration, the service's requirements, and the candidate resources per
 * requirement (with their capabilities, availability, blocks and existing
 * reservations) — captured as immutable data. The engine reads a snapshot and
 * returns ranked candidates without mutating anything.
 *
 * <p>
 * The application layer builds a snapshot from tenant-scoped repositories; the
 * engine never touches HTTP, JPA or Firebase (ADR-001, enforced by
 * ArchitectureTest).
 *
 * @param requestWindow
 *            the window within which to search (already clamped to the horizon)
 * @param serviceDuration
 *            the nominal duration of the service being booked
 * @param requirements
 *            the service's resource requirements
 * @param candidatesByRequirement
 *            candidate resources keyed by requirement id
 * @param parameters
 *            search tuning (increment, result cap)
 */
public record SchedulingSnapshot(Interval requestWindow, Duration serviceDuration, List<RequirementSpec> requirements,
		Map<String, List<ResourceCandidate>> candidatesByRequirement, SearchParameters parameters) {

	public SchedulingSnapshot {
		Objects.requireNonNull(requestWindow, "requestWindow");
		Objects.requireNonNull(serviceDuration, "serviceDuration");
		if (serviceDuration.isZero() || serviceDuration.isNegative()) {
			throw new IllegalArgumentException("serviceDuration must be positive");
		}
		requirements = List.copyOf(requirements);
		candidatesByRequirement = Map.copyOf(candidatesByRequirement);
		Objects.requireNonNull(parameters, "parameters");
	}

	List<ResourceCandidate> candidatesFor(RequirementSpec requirement) {
		return candidatesByRequirement.getOrDefault(requirement.requirementId(), List.of());
	}
}
