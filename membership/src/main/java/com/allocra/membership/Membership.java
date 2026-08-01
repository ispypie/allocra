package com.allocra.membership;

import com.allocra.common.tenant.TenantId;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * An {@code ApplicationUser}'s validated membership of a tenant (PRD-MEM-001),
 * carrying the roles that grant permissions. Resolved from PostgreSQL, never
 * trusted from the client (PRD-TEN-004 / PRD-SEC-002).
 *
 * @param tenantId
 *            the tenant this membership belongs to
 * @param memberId
 *            the organisation-member id
 * @param userId
 *            the application-user id
 * @param roles
 *            the member's roles
 */
public record Membership(TenantId tenantId, UUID memberId, UUID userId, Set<Role> roles) {

	public Membership {
		Objects.requireNonNull(tenantId, "tenantId");
		Objects.requireNonNull(memberId, "memberId");
		Objects.requireNonNull(userId, "userId");
		roles = Set.copyOf(roles);
	}

	/** The union of permissions granted by this member's roles (PRD-MEM-003). */
	public Set<Permission> permissions() {
		EnumSet<Permission> all = EnumSet.noneOf(Permission.class);
		for (Role role : roles) {
			all.addAll(role.permissions());
		}
		return all;
	}

	public boolean has(Permission permission) {
		return permissions().contains(permission);
	}
}
