package com.allocra.app.web;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Request/response DTOs for the configuration API (PRD-RES-010, PRD-SVC-008,
 * PRD-AVL-004).
 */
public final class ConfigApi {

	private ConfigApi() {
	}

	public record IdResponse(UUID id) {
	}

	public record CreateLocationRequest(String name, String timezone) {
	}

	public record CreateOperatingHoursRequest(int dayOfWeek, LocalTime openTime, LocalTime closeTime) {
	}

	public record CreateClosureRequest(LocalDate startDate, LocalDate endDate, String reason) {
	}

	public record CreateResourceTypeRequest(String code, String baseKind) {
	}

	public record CreateResourceRequest(UUID resourceTypeId, String name, UUID locationId, String mobility,
			List<CapabilitySpecDto> capabilities, List<UUID> compatibleResourceIds) {
	}

	public record CapabilitySpecDto(String type, Integer level, LocalDate validFrom, LocalDate validTo) {
	}

	public record CreateServiceRequest(String code, String name, int durationMinutes,
			List<RequirementSpecDto> requirements) {
	}

	public record RequirementSpecDto(String baseKind, boolean required, String selectionMode,
			String requiredCapabilityType, Integer minLevel) {
	}

	public record CreatedServiceResponse(UUID serviceTypeId, List<CreatedRequirement> requirements) {
	}

	public record CreatedRequirement(UUID requirementId, String baseKind) {
	}

	public record CreateAvailabilityRuleRequest(int dayOfWeek, LocalTime startTime, LocalTime endTime) {
	}
}
