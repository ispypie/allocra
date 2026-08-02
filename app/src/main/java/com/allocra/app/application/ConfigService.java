package com.allocra.app.application;

import com.allocra.app.persistence.ConfigWriteRepository;
import com.allocra.app.web.ConfigApi.CapabilitySpecDto;
import com.allocra.app.web.ConfigApi.CreateResourceRequest;
import com.allocra.app.web.ConfigApi.CreateServiceRequest;
import com.allocra.app.web.ConfigApi.CreatedRequirement;
import com.allocra.app.web.ConfigApi.CreatedServiceResponse;
import com.allocra.app.web.ConfigApi.RequirementSpecDto;
import com.allocra.common.error.NotAuthorizedException;
import com.allocra.common.tenant.TenantId;
import com.allocra.membership.Membership;
import com.allocra.membership.Permission;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuration use cases (PRD-RES-010, PRD-SVC-008, PRD-AVL-004): create the
 * tenant-scoped locations, resource types, resources, services and availability
 * that make a service bookable — all over the API, permission-checked, without
 * SQL seeding.
 */
@Service
public class ConfigService {

	private final ConfigWriteRepository config;

	public ConfigService(ConfigWriteRepository config) {
		this.config = config;
	}

	@Transactional
	public UUID createLocation(TenantId tenantId, Membership membership, String name, String timezone) {
		require(membership, Permission.RESOURCE_MANAGE);
		UUID id = UUID.randomUUID();
		config.insertLocation(tenantId, id, name, timezone == null || timezone.isBlank() ? "UTC" : timezone);
		return id;
	}

	@Transactional
	public UUID createOperatingHours(TenantId tenantId, Membership membership, UUID locationId, int dayOfWeek,
			LocalTime openTime, LocalTime closeTime) {
		require(membership, Permission.AVAILABILITY_MANAGE);
		UUID id = UUID.randomUUID();
		config.insertOperatingHours(tenantId, id, locationId, dayOfWeek, openTime, closeTime);
		return id;
	}

	@Transactional
	public UUID createClosure(TenantId tenantId, Membership membership, UUID locationId, LocalDate startDate,
			LocalDate endDate, String reason) {
		require(membership, Permission.AVAILABILITY_MANAGE);
		UUID id = UUID.randomUUID();
		config.insertClosure(tenantId, id, locationId, startDate, endDate, reason);
		return id;
	}

	@Transactional
	public UUID createResourceType(TenantId tenantId, Membership membership, String code, String baseKind) {
		require(membership, Permission.RESOURCE_MANAGE);
		UUID id = UUID.randomUUID();
		config.insertResourceType(tenantId, id, code, baseKind);
		return id;
	}

	@Transactional
	public UUID createResource(TenantId tenantId, Membership membership, CreateResourceRequest request) {
		require(membership, Permission.RESOURCE_MANAGE);
		UUID id = UUID.randomUUID();
		config.insertResource(tenantId, id, request.resourceTypeId(), request.name(), request.locationId(),
				request.mobility(), request.setupMinutes(), request.cleanupMinutes());
		if (request.capabilities() != null) {
			for (CapabilitySpecDto c : request.capabilities()) {
				config.insertCapability(tenantId, UUID.randomUUID(), id, c.type(), c.level(), c.validFrom(),
						c.validTo());
			}
		}
		if (request.compatibleResourceIds() != null) {
			for (UUID other : request.compatibleResourceIds()) {
				config.insertCompatibility(tenantId, id, other);
			}
		}
		return id;
	}

	@Transactional
	public UUID createAvailabilityRule(TenantId tenantId, Membership membership, UUID resourceId, int dayOfWeek,
			LocalTime start, LocalTime end) {
		require(membership, Permission.AVAILABILITY_MANAGE);
		UUID id = UUID.randomUUID();
		config.insertAvailabilityRule(tenantId, id, resourceId, dayOfWeek, start, end);
		return id;
	}

	@Transactional
	public CreatedServiceResponse createService(TenantId tenantId, Membership membership,
			CreateServiceRequest request) {
		require(membership, Permission.SERVICE_MANAGE);
		UUID serviceTypeId = UUID.randomUUID();
		config.insertServiceType(tenantId, serviceTypeId, request.code(), request.name(), request.durationMinutes(),
				request.leadMinutes());
		List<CreatedRequirement> created = new ArrayList<>();
		if (request.requirements() != null) {
			for (RequirementSpecDto r : request.requirements()) {
				UUID requirementId = UUID.randomUUID();
				config.insertRequirement(tenantId, requirementId, serviceTypeId, r.baseKind(), r.required(),
						r.selectionMode(), r.requiredCapabilityType(), r.minLevel());
				created.add(new CreatedRequirement(requirementId, r.baseKind()));
			}
		}
		return new CreatedServiceResponse(serviceTypeId, created);
	}

	private static void require(Membership membership, Permission permission) {
		if (!membership.has(permission)) {
			throw new NotAuthorizedException("missing permission " + permission);
		}
	}
}
