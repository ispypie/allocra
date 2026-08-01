package com.allocra.app.persistence;

import com.allocra.common.tenant.TenantId;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Tenant-scoped inserts for configuration (PRD-RES-010, PRD-SVC-008,
 * PRD-AVL-004).
 */
@Repository
public class ConfigWriteRepository {

	private final JdbcClient jdbc;

	public ConfigWriteRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public void insertLocation(TenantId tenantId, UUID id, String name) {
		jdbc.sql("INSERT INTO location(tenant_id, id, name) VALUES (?,?,?)").params(tenantId.value(), id, name)
				.update();
	}

	public void insertResourceType(TenantId tenantId, UUID id, String code, String baseKind) {
		jdbc.sql("INSERT INTO resource_type(tenant_id, id, code, base_kind) VALUES (?,?,?,?)")
				.params(tenantId.value(), id, code, baseKind).update();
	}

	public void insertResource(TenantId tenantId, UUID id, UUID resourceTypeId, String name, UUID locationId,
			String mobility) {
		jdbc.sql(
				"INSERT INTO resource(tenant_id, id, resource_type_id, name, location_id, mobility) VALUES (?,?,?,?,?,?)")
				.param(tenantId.value()).param(id).param(resourceTypeId).param(name).param(locationId).param(mobility)
				.update();
	}

	public void insertCapability(TenantId tenantId, UUID id, UUID resourceId, String type, Integer level,
			LocalDate validFrom, LocalDate validTo) {
		jdbc.sql(
				"INSERT INTO resource_capability(tenant_id, id, resource_id, capability_type, level, valid_from, valid_to)"
						+ " VALUES (?,?,?,?,?,?,?)")
				.param(tenantId.value()).param(id).param(resourceId).param(type).param(level).param(validFrom)
				.param(validTo).update();
	}

	public void insertCompatibility(TenantId tenantId, UUID resourceId, UUID compatibleResourceId) {
		jdbc.sql("INSERT INTO resource_compatibility(tenant_id, resource_id, compatible_resource_id) VALUES (?,?,?)")
				.params(tenantId.value(), resourceId, compatibleResourceId).update();
	}

	public void insertServiceType(TenantId tenantId, UUID id, String code, String name, int durationMinutes) {
		jdbc.sql("INSERT INTO service_type(tenant_id, id, code, name, duration_minutes) VALUES (?,?,?,?,?)")
				.params(tenantId.value(), id, code, name, durationMinutes).update();
	}

	public void insertRequirement(TenantId tenantId, UUID id, UUID serviceTypeId, String baseKind, boolean required,
			String selectionMode, String requiredCapabilityType, Integer minLevel) {
		jdbc.sql("INSERT INTO resource_requirement(tenant_id, id, service_type_id, base_kind, required, selection_mode,"
				+ " required_capability_type, min_level) VALUES (?,?,?,?,?,?,?,?)").param(tenantId.value()).param(id)
				.param(serviceTypeId).param(baseKind).param(required).param(selectionMode).param(requiredCapabilityType)
				.param(minLevel).update();
	}

	public void insertAvailabilityRule(TenantId tenantId, UUID id, UUID resourceId, int dayOfWeek, LocalTime startTime,
			LocalTime endTime) {
		jdbc.sql("INSERT INTO availability_rule(tenant_id, id, resource_id, day_of_week, start_time, end_time)"
				+ " VALUES (?,?,?,?,?,?)").params(tenantId.value(), id, resourceId, dayOfWeek, startTime, endTime)
				.update();
	}
}
