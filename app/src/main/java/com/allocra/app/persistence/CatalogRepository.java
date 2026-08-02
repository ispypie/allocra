package com.allocra.app.persistence;

import com.allocra.common.tenant.TenantId;
import com.allocra.scheduling.BaseKind;
import com.allocra.scheduling.CapabilityRequirement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Tenant-scoped reads of service definitions and their requirements
 * (PRD-SVC-001).
 */
@Repository
public class CatalogRepository {

	private final JdbcClient jdbc;

	public CatalogRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public Optional<ServiceDefinition> findService(TenantId tenantId, UUID serviceTypeId) {
		Optional<int[]> durations = jdbc
				.sql("SELECT duration_minutes, lead_minutes FROM service_type WHERE tenant_id = ? AND id = ?")
				.param(tenantId.value()).param(serviceTypeId)
				.query((rs, n) -> new int[]{rs.getInt("duration_minutes"), rs.getInt("lead_minutes")}).optional();
		if (durations.isEmpty()) {
			return Optional.empty();
		}
		List<RequirementRow> requirements = jdbc.sql("""
				SELECT id, base_kind, required, selection_mode, required_capability_type, min_level
				FROM resource_requirement
				WHERE tenant_id = ? AND service_type_id = ?
				ORDER BY id
				""").param(tenantId.value()).param(serviceTypeId).query((rs, n) -> {
			String capType = rs.getString("required_capability_type");
			Integer minLevel = (Integer) rs.getObject("min_level");
			CapabilityRequirement capability = capType == null ? null : new CapabilityRequirement(capType, minLevel);
			return new RequirementRow(rs.getObject("id", UUID.class), BaseKind.valueOf(rs.getString("base_kind")),
					rs.getBoolean("required"), rs.getString("selection_mode"), capability);
		}).list();
		return Optional.of(new ServiceDefinition(serviceTypeId, durations.get()[0], durations.get()[1], requirements));
	}

	/** A service and its requirements. */
	public record ServiceDefinition(UUID serviceTypeId, int durationMinutes, int leadMinutes,
			List<RequirementRow> requirements) {
		public ServiceDefinition {
			requirements = List.copyOf(requirements);
		}
	}

	/**
	 * One requirement row (specific-resource preferences come from the request,
	 * PRD-SVC-005).
	 */
	public record RequirementRow(UUID id, BaseKind kind, boolean required, String selectionMode,
			CapabilityRequirement capability) {
	}
}
