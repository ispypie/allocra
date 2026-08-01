package com.allocra.scheduling;

import java.util.List;
import java.util.Objects;

/**
 * A ranked, feasible option returned by direct availability search
 * (PRD-SCH-004): a time slot plus one assignment per satisfied requirement, and
 * a soft score for ranking (higher is better). Every hard requirement is
 * assigned; optional requirements may be absent (PRD-SVC-003).
 *
 * @param slot
 *            the proposed time window
 * @param assignments
 *            the proposed requirement→resource bindings
 * @param score
 *            the soft-constraint score (PRD-SCH-003); higher ranks first
 */
public record CandidateOption(Interval slot, List<AssignmentChoice> assignments, double score) {

	public CandidateOption {
		Objects.requireNonNull(slot, "slot");
		assignments = List.copyOf(assignments);
	}
}
