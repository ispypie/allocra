package com.allocra.scheduling;

import java.util.Objects;
import java.util.Set;

/**
 * An immutable description of one resource requirement of a service
 * (PRD-SVC-001/002). It describes <em>what</em> is needed (kind + capability),
 * not a specific named resource, unless the request explicitly requires one via
 * {@link #requiredResourceIds()} (PRD-SVC-005).
 *
 * @param requirementId
 *            opaque requirement identifier
 * @param kind
 *            the base kind of resource needed (PRD-RES-002)
 * @param required
 *            whether this requirement must be satisfied for confirmation
 *            (PRD-SVC-003)
 * @param selectionMode
 *            how specific-resource preferences are treated (PRD-SVC-005)
 * @param capability
 *            optional capability requirement (PRD-SVC-006); {@code null} means
 *            none
 * @param requiredResourceIds
 *            allowlist for {@link SelectionMode#REQUIRED}; empty means any
 *            capable resource is acceptable (hard constraint when non-empty)
 * @param preferredResourceIds
 *            resources to rank higher (soft constraint, PRD-SCH-003)
 */
public record RequirementSpec(String requirementId, BaseKind kind, boolean required, SelectionMode selectionMode,
		CapabilityRequirement capability, Set<String> requiredResourceIds, Set<String> preferredResourceIds) {

	public RequirementSpec {
		Objects.requireNonNull(requirementId, "requirementId");
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(selectionMode, "selectionMode");
		requiredResourceIds = Set.copyOf(requiredResourceIds);
		preferredResourceIds = Set.copyOf(preferredResourceIds);
	}

	/**
	 * True if {@code resourceId} is permitted by this requirement's hard resource
	 * allowlist.
	 */
	public boolean allows(String resourceId) {
		return requiredResourceIds.isEmpty() || requiredResourceIds.contains(resourceId);
	}

	/**
	 * True if {@code resourceId} is preferred (soft ranking, PRD-SVC-005
	 * PREFERRED).
	 */
	public boolean prefers(String resourceId) {
		return preferredResourceIds.contains(resourceId);
	}
}
