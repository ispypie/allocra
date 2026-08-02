package com.allocra.scheduling;

import java.time.Duration;
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
 *            (PRD-RSV-001); these already include any buffers of the bookings
 *            that created them
 * @param compatibleResourceIds
 *            allowlist of resource ids this resource may be combined with;
 *            empty means no restriction (PRD-RES-008; e.g. FIXED equipment
 *            lists its room)
 * @param setupMinutes
 *            minutes of setup consumed before the appointment (PRD-RES-012)
 * @param cleanupMinutes
 *            minutes of cleanup consumed after the appointment (PRD-RES-012)
 */
public record ResourceCandidate(String resourceId, BaseKind kind, String resourceTypeId,
		List<CapabilitySpec> capabilities, List<Interval> availabilityWindows, List<Interval> blocked,
		List<Interval> reservations, Set<String> compatibleResourceIds, int setupMinutes, int cleanupMinutes) {

	public ResourceCandidate {
		Objects.requireNonNull(resourceId, "resourceId");
		Objects.requireNonNull(kind, "kind");
		capabilities = List.copyOf(capabilities);
		availabilityWindows = List.copyOf(availabilityWindows);
		blocked = List.copyOf(blocked);
		reservations = List.copyOf(reservations);
		compatibleResourceIds = Set.copyOf(compatibleResourceIds);
		if (setupMinutes < 0 || cleanupMinutes < 0) {
			throw new IllegalArgumentException("setup/cleanup minutes must be >= 0");
		}
	}

	/** Convenience for a resource with no setup/cleanup buffers. */
	public ResourceCandidate(String resourceId, BaseKind kind, String resourceTypeId, List<CapabilitySpec> capabilities,
			List<Interval> availabilityWindows, List<Interval> blocked, List<Interval> reservations,
			Set<String> compatibleResourceIds) {
		this(resourceId, kind, resourceTypeId, capabilities, availabilityWindows, blocked, reservations,
				compatibleResourceIds, 0, 0);
	}

	/**
	 * True if the resource is available for the whole appointment slot: some
	 * availability window contains it and no block overlaps it (PRD-AVL-003).
	 * Reservation conflicts are checked separately against the buffered window (see
	 * {@link #reservedFree}).
	 */
	public boolean withinAvailability(Interval slot) {
		boolean covered = availabilityWindows.stream().anyMatch(w -> w.contains(slot));
		return covered && blocked.stream().noneMatch(b -> b.overlaps(slot));
	}

	/** True if no existing reservation overlaps {@code window} (PRD-RSV-002). */
	public boolean reservedFree(Interval window) {
		return reservations.stream().noneMatch(r -> r.overlaps(window));
	}

	/**
	 * The window this resource is actually consumed for, given an appointment
	 * {@code slot} and a service lead time:
	 * {@code [start − lead − setup, end + cleanup)} (PRD-SVC-009, PRD-RES-012).
	 * This is what a reservation is written for, and what contends with other
	 * bookings.
	 */
	public Interval reservedWindow(Interval slot, int leadMinutes) {
		return new Interval(slot.start().minus(Duration.ofMinutes((long) leadMinutes + setupMinutes)),
				slot.end().plus(Duration.ofMinutes(cleanupMinutes)));
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
