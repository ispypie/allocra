package com.allocra.app.web;

import com.allocra.app.application.ConfigService;
import com.allocra.app.web.ConfigApi.CreateAvailabilityRuleRequest;
import com.allocra.app.web.ConfigApi.CreateLocationRequest;
import com.allocra.app.web.ConfigApi.CreateResourceRequest;
import com.allocra.app.web.ConfigApi.CreateResourceTypeRequest;
import com.allocra.app.web.ConfigApi.CreateServiceRequest;
import com.allocra.app.web.ConfigApi.CreatedServiceResponse;
import com.allocra.app.web.ConfigApi.IdResponse;
import com.allocra.common.tenant.TenantContext;
import com.allocra.common.tenant.TenantId;
import com.allocra.membership.Membership;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Configuration API (PRD-RES-010, PRD-SVC-008, PRD-AVL-004): create the
 * tenant's locations, resource types, resources, services and availability over
 * HTTP. Permission checks live in {@link ConfigService}; all endpoints return
 * 201.
 */
@RestController
@RequestMapping("/v1")
public class ConfigController {

	private final ConfigService config;

	public ConfigController(ConfigService config) {
		this.config = config;
	}

	@PostMapping("/locations")
	@ResponseStatus(HttpStatus.CREATED)
	public IdResponse createLocation(@RequestBody CreateLocationRequest request,
			@RequestAttribute(TenantAuthFilter.MEMBERSHIP_ATTRIBUTE) Membership membership) {
		return new IdResponse(config.createLocation(tenant(), membership, request.name()));
	}

	@PostMapping("/resource-types")
	@ResponseStatus(HttpStatus.CREATED)
	public IdResponse createResourceType(@RequestBody CreateResourceTypeRequest request,
			@RequestAttribute(TenantAuthFilter.MEMBERSHIP_ATTRIBUTE) Membership membership) {
		return new IdResponse(config.createResourceType(tenant(), membership, request.code(), request.baseKind()));
	}

	@PostMapping("/resources")
	@ResponseStatus(HttpStatus.CREATED)
	public IdResponse createResource(@RequestBody CreateResourceRequest request,
			@RequestAttribute(TenantAuthFilter.MEMBERSHIP_ATTRIBUTE) Membership membership) {
		return new IdResponse(config.createResource(tenant(), membership, request));
	}

	@PostMapping("/resources/{resourceId}/availability-rules")
	@ResponseStatus(HttpStatus.CREATED)
	public IdResponse createAvailabilityRule(@PathVariable UUID resourceId,
			@RequestBody CreateAvailabilityRuleRequest request,
			@RequestAttribute(TenantAuthFilter.MEMBERSHIP_ATTRIBUTE) Membership membership) {
		return new IdResponse(config.createAvailabilityRule(tenant(), membership, resourceId, request.dayOfWeek(),
				request.startTime(), request.endTime()));
	}

	@PostMapping("/services")
	@ResponseStatus(HttpStatus.CREATED)
	public CreatedServiceResponse createService(@RequestBody CreateServiceRequest request,
			@RequestAttribute(TenantAuthFilter.MEMBERSHIP_ATTRIBUTE) Membership membership) {
		return config.createService(tenant(), membership, request);
	}

	private static TenantId tenant() {
		return TenantContext.require();
	}
}
