package com.allocra.app.persistence;

import com.allocra.common.tenant.TenantId;
import com.allocra.membership.Membership;
import com.allocra.membership.Role;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Tenant-scoped lookups for membership and authorization (PRD-TEN-003,
 * ADR-002). The active membership is resolved from PostgreSQL by (tenant,
 * Firebase UID); a caller with no ACTIVE membership in the requested tenant
 * gets nothing back — the client's claimed tenant is never trusted (PRD-TEN-004
 * / PRD-SEC-002).
 */
@Repository
public class AuthRepository {

	private final JdbcClient jdbc;

	public AuthRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public Optional<Membership> findActiveMembership(TenantId tenantId, String firebaseUid) {
		Optional<UUID[]> memberAndUser = jdbc.sql("""
				SELECT m.id, m.user_id
				FROM organisation_member m
				JOIN application_user u ON u.id = m.user_id
				WHERE m.tenant_id = ? AND u.firebase_uid = ? AND m.status = 'ACTIVE'
				""").param(tenantId.value()).param(firebaseUid)
				.query((rs, n) -> new UUID[]{rs.getObject(1, UUID.class), rs.getObject(2, UUID.class)}).optional();

		if (memberAndUser.isEmpty()) {
			return Optional.empty();
		}
		UUID memberId = memberAndUser.get()[0];
		UUID userId = memberAndUser.get()[1];

		Set<Role> roles = EnumSet.noneOf(Role.class);
		jdbc.sql("SELECT role FROM organisation_member_role WHERE tenant_id = ? AND member_id = ?")
				.param(tenantId.value()).param(memberId).query((rs, n) -> Role.valueOf(rs.getString(1))).list()
				.forEach(roles::add);

		return Optional.of(new Membership(tenantId, memberId, userId, roles));
	}
}
