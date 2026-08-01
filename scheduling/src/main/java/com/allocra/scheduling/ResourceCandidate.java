package com.allocra.scheduling;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * An immutable snapshot of a single candidate resource, as seen by the pure
 * engine (PRD-SCH-001). It carries only what scheduling needs — no JPA
 * entities, no infrastructure.
 *
 * @param resourceId
 *            opaque resource identifier
 * @param kind
 *            the resource's base kind (PRD-RES-002)
 * @param resourceTypeId
 *            opaque resource-type identifier (for reference only; the engine
 *            does not branch on concrete types — ADR-001)
 * @param capabilities
 *            capabilities the resource possesses (PRD-RES-004)
 * @param availabilityWindows
 *            windows the resource is available (PRD-AVL-001)
 * @param blocked
 *            explicit unavailability overriding availability (PRD-AVL-002)
 * @param reservations
 *            existing exclusive reservations consuming the resource
 *            (PRD-RSV-001)
 * @param compatibleResourceIds
 *            allowlist of resource ids this resource may be combined with;
 *            empty means no restriction (PRD-RES-008; e.g. FIXED equipment
 *            lists its room)
 */
public record ResourceCandidate(String resourceId, BaseKind kind, String resourceTypeId,
		List<CapabilitySpec> capabilities, List<Interval> availabilityWindows, List<Interval> blocked,
		List<Interval> reservations, Set<String> compatibleResourceIds) {

	public ResourceCandidate {
		Objects.requireNonNull(resourceId, "resourceId");
		Objects.requireNonNull(kind, "kind");
		capabilities = List.copyOf(capabilities);
		availabilityWindows = List.copyOf(availabilityWindows);
		blocked = List.copyOf(blocked);
		reservations = List.copyOf(reservations);
		compatibleResourceIds = Set.copyOf(compatibleResourceIds);
	}

	/**
	 * True if the resource is free for the whole slot: some availability window
	 * contains it, and no block or existing reservation overlaps it (PRD-AVL-003,
	 * PRD-RSV-002).
	 */
	public boolean availableFor(Interval slot) {
		boolean withinAvailability = availabilityWindows.stream().anyMatch(w -> w.contains(slot));
		if (!withinAvailability) {
			return false;
		}
		boolean blockedOut = blocked.stream().anyMatch(b -> b.overlaps(slot));
		boolean reserved = reservations.stream().anyMatch(r -> r.overlaps(slot));
		return !blockedOut && !reserved;
	}

	/**
	 * True if the resource holds a capability satisfying {@code requirement} on the
	 * date.
	 */
	public boolean hasCapability(CapabilityRequirement requirement, LocalDate appointmentDate) {
		return capabilities.stream().anyMatch(c -> requirement.satisfiedBy(c, appointmentDate));
	}

	/**
	 * True if this resource may be combined with {@code otherResourceId}
	 * (PRD-RES-008).
	 */
	public boolean compatibleWith(String otherResourceId) {
		return compatibleResourceIds.isEmpty() || compatibleResourceIds.contains(otherResourceId);
	}
}
