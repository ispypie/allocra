package com.allocra.membership;

import java.util.Set;

/**
 * Initial roles (PRD-MEM-002). Each role maps to a fixed set of
 * {@link Permission}s. For the vertical slice this mapping is a static domain
 * policy (DEC-020); a configurable role/permission store can replace it later
 * without changing call sites, because callers check permissions, not roles
 * (PRD-MEM-003).
 */
public enum Role {
	ORG_ADMIN(Set.of(Permission.values())), SCHEDULER(Set.of(Permission.BOOKING_CREATE, Permission.BOOKING_VIEW,
			Permission.BOOKING_CANCEL, Permission.BOOKING_RESCHEDULE, Permission.BOOKING_UPDATE,
			Permission.AVAILABILITY_MANAGE)), RESOURCE_MEMBER(
					Set.of(Permission.BOOKING_VIEW)), VIEWER(Set.of(Permission.BOOKING_VIEW));

	private final Set<Permission> permissions;

	Role(Set<Permission> permissions) {
		this.permissions = Set.copyOf(permissions);
	}

	public Set<Permission> permissions() {
		return permissions;
	}
}
