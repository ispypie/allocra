package com.allocra.scheduling;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Direct availability search (PRD-SCH-004): finds feasible, ranked options for
 * a service within a request window <strong>without changing any existing
 * booking or assignment</strong> (PRD-ASN-004). Existing reservations are
 * treated purely as blocking — the engine never frees or reassigns them (that
 * is schedule repair, deferred: PRD-SCH-007).
 *
 * <p>
 * This class is stateless: it holds no fields and no authoritative calendar
 * state (PRD-SCH-001, PRD-NFR-001). It reads a {@link SchedulingSnapshot} and
 * returns options.
 *
 * <p>
 * The algorithm generates candidate start times across the window at a fixed
 * increment, and for each slot resolves a consistent combination of resources
 * (one per requirement) that satisfies every hard constraint — capability +
 * validity (PRD-SVC-006), availability (PRD-AVL-003), required-resource
 * allowlist (PRD-SVC-005), and cross-requirement compatibility (PRD-RES-008).
 * Options are ranked by soft score (PRD-SCH-003), then earliest slot, and
 * capped at {@link SearchParameters#maxResults()}.
 */
public final class DirectAvailabilitySearch {

	private static final double PREFERRED_BONUS = 1.0;

	public List<CandidateOption> search(SchedulingSnapshot snapshot) {
		List<CandidateOption> options = new ArrayList<>();
		for (Interval slot : generateSlots(snapshot)) {
			LocalDate appointmentDate = slot.start().atZone(ZoneOffset.UTC).toLocalDate();
			buildOptionForSlot(snapshot, slot, appointmentDate).ifPresent(options::add);
		}
		options.sort(
				Comparator.comparingDouble(CandidateOption::score).reversed().thenComparing(o -> o.slot().start()));
		int limit = Math.min(options.size(), snapshot.parameters().maxResults());
		return List.copyOf(options.subList(0, limit));
	}

	private List<Interval> generateSlots(SchedulingSnapshot snapshot) {
		List<Interval> slots = new ArrayList<>();
		Duration duration = snapshot.serviceDuration();
		Duration increment = snapshot.parameters().increment();
		Instant windowEnd = snapshot.requestWindow().end();
		Instant start = snapshot.requestWindow().start();
		while (!start.plus(duration).isAfter(windowEnd)) {
			slots.add(new Interval(start, start.plus(duration)));
			start = start.plus(increment);
		}
		return slots;
	}

	/**
	 * Attempts to resolve a consistent assignment for every hard requirement at
	 * {@code slot}, choosing higher-scoring (preferred) resources first, and
	 * returns the best option found.
	 */
	private java.util.Optional<CandidateOption> buildOptionForSlot(SchedulingSnapshot snapshot, Interval slot,
			LocalDate appointmentDate) {
		Map<String, ResourceCandidate> chosen = new LinkedHashMap<>();
		boolean resolved = assign(snapshot.requirements(), 0, snapshot, slot, appointmentDate, chosen);
		if (!resolved) {
			return java.util.Optional.empty();
		}
		List<AssignmentChoice> assignments = new ArrayList<>();
		double score = 0.0;
		for (RequirementSpec requirement : snapshot.requirements()) {
			ResourceCandidate resource = chosen.get(requirement.requirementId());
			if (resource == null) {
				continue; // optional requirement left unsatisfied
			}
			assignments.add(new AssignmentChoice(requirement.requirementId(), resource.resourceId()));
			if (requirement.prefers(resource.resourceId())) {
				score += PREFERRED_BONUS;
			}
		}
		return java.util.Optional.of(new CandidateOption(slot, assignments, score));
	}

	/**
	 * Depth-first assignment across requirements with backtracking. Required
	 * requirements must be assigned a compatible feasible resource; optional
	 * requirements may be skipped when no compatible feasible resource exists
	 * (PRD-SVC-003).
	 */
	private boolean assign(List<RequirementSpec> requirements, int index, SchedulingSnapshot snapshot, Interval slot,
			LocalDate appointmentDate, Map<String, ResourceCandidate> chosen) {
		if (index == requirements.size()) {
			return true;
		}
		RequirementSpec requirement = requirements.get(index);
		for (ResourceCandidate candidate : orderedFeasible(requirement, snapshot, slot, appointmentDate)) {
			if (compatibleWithChosen(candidate, chosen)) {
				chosen.put(requirement.requirementId(), candidate);
				if (assign(requirements, index + 1, snapshot, slot, appointmentDate, chosen)) {
					return true;
				}
				chosen.remove(requirement.requirementId());
			}
		}
		// Optional requirements may be left unsatisfied and still yield a feasible
		// option.
		if (!requirement.required()) {
			return assign(requirements, index + 1, snapshot, slot, appointmentDate, chosen);
		}
		return false;
	}

	/**
	 * Feasible candidates for a requirement at a slot, preferred ones first
	 * (best-score-first).
	 */
	private List<ResourceCandidate> orderedFeasible(RequirementSpec requirement, SchedulingSnapshot snapshot,
			Interval slot, LocalDate appointmentDate) {
		List<ResourceCandidate> feasible = new ArrayList<>();
		for (ResourceCandidate candidate : snapshot.candidatesFor(requirement)) {
			if (isFeasible(requirement, candidate, slot, appointmentDate)) {
				feasible.add(candidate);
			}
		}
		feasible.sort(Comparator.comparing((ResourceCandidate c) -> requirement.prefers(c.resourceId()) ? 0 : 1));
		return feasible;
	}

	/** Hard-constraint check for a single (requirement, resource) at a slot. */
	private boolean isFeasible(RequirementSpec requirement, ResourceCandidate candidate, Interval slot,
			LocalDate appointmentDate) {
		if (candidate.kind() != requirement.kind()) {
			return false;
		}
		if (!requirement.allows(candidate.resourceId())) {
			return false; // REQUIRED specific-resource hard constraint (PRD-SVC-005)
		}
		if (requirement.capability() != null && !candidate.hasCapability(requirement.capability(), appointmentDate)) {
			return false; // qualification / validity (PRD-SVC-006)
		}
		return candidate.availableFor(slot); // availability ∩ ¬blocks ∩ ¬reservations (PRD-AVL-003)
	}

	private boolean compatibleWithChosen(ResourceCandidate candidate, Map<String, ResourceCandidate> chosen) {
		for (ResourceCandidate other : chosen.values()) {
			if (!candidate.compatibleWith(other.resourceId()) || !other.compatibleWith(candidate.resourceId())) {
				return false;
			}
		}
		return true;
	}
}
