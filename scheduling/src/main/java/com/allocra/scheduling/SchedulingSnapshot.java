package com.allocra.scheduling;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The immutable input to the scheduling engine (PRD-SCH-001). It carries
 * everything needed to compute candidate options — the request window and
 * service duration, the service's requirements, the candidate resources per
 * requirement, and the service lead time — captured as immutable data. The
 * engine reads a snapshot and returns ranked candidates without mutating
 * anything.
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
 * @param leadMinutes
 *            service lead/prep time applied before every appointment
 *            (PRD-SVC-009)
 */
public record SchedulingSnapshot(Interval requestWindow, Duration serviceDuration, List<RequirementSpec> requirements,
		Map<String, List<ResourceCandidate>> candidatesByRequirement, SearchParameters parameters, int leadMinutes) {

	public SchedulingSnapshot {
		Objects.requireNonNull(requestWindow, "requestWindow");
		Objects.requireNonNull(serviceDuration, "serviceDuration");
		if (serviceDuration.isZero() || serviceDuration.isNegative()) {
			throw new IllegalArgumentException("serviceDuration must be positive");
		}
		requirements = List.copyOf(requirements);
		candidatesByRequirement = Map.copyOf(candidatesByRequirement);
		Objects.requireNonNull(parameters, "parameters");
		if (leadMinutes < 0) {
			throw new IllegalArgumentException("leadMinutes must be >= 0");
		}
	}

	/** Convenience for a service with no lead time. */
	public SchedulingSnapshot(Interval requestWindow, Duration serviceDuration, List<RequirementSpec> requirements,
			Map<String, List<ResourceCandidate>> candidatesByRequirement, SearchParameters parameters) {
		this(requestWindow, serviceDuration, requirements, candidatesByRequirement, parameters, 0);
	}

	List<ResourceCandidate> candidatesFor(RequirementSpec requirement) {
		return candidatesByRequirement.getOrDefault(requirement.requirementId(), List.of());
	}

	/**
	 * The candidate for a specific requirement + resource, if present (used at
	 * confirmation).
	 */
	public ResourceCandidate candidateFor(String requirementId, String resourceId) {
		return candidatesByRequirement.getOrDefault(requirementId, List.of()).stream()
				.filter(c -> c.resourceId().equals(resourceId)).findFirst().orElse(null);
	}
}
