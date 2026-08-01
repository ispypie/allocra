package com.allocra.app.web;

import com.allocra.app.application.AvailabilitySearchService;
import com.allocra.app.application.SchedulingSnapshotBuilder.RequirementPreference;
import com.allocra.app.web.ApiModel.AssignmentDto;
import com.allocra.app.web.ApiModel.OptionDto;
import com.allocra.app.web.ApiModel.RequirementPreferenceDto;
import com.allocra.app.web.ApiModel.SearchRequest;
import com.allocra.app.web.ApiModel.SearchResponse;
import com.allocra.common.tenant.TenantContext;
import com.allocra.common.tenant.TenantId;
import com.allocra.scheduling.CandidateOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Direct availability search endpoint (PRD-SCH-004, PRD-BKG-003 INTERNAL).
 * Returns bookable <em>options</em> (not raw resource calendars), keeping the
 * shape self-service-ready (ADR-005).
 */
@RestController
@RequestMapping("/v1/services")
public class AvailabilityController {

	private final AvailabilitySearchService searchService;

	public AvailabilityController(AvailabilitySearchService searchService) {
		this.searchService = searchService;
	}

	@PostMapping("/{serviceTypeId}/availability/search")
	public SearchResponse search(@PathVariable UUID serviceTypeId, @RequestBody SearchRequest request) {
		TenantId tenantId = TenantContext.require();
		List<CandidateOption> options = searchService.search(tenantId, serviceTypeId, request.from(), request.to(),
				preferences(request));
		return new SearchResponse(options.stream()
				.map(o -> new OptionDto(o.slot().start(), o.slot().end(), assignments(o), o.score())).toList());
	}

	private static Map<UUID, RequirementPreference> preferences(SearchRequest request) {
		Map<UUID, RequirementPreference> prefs = new HashMap<>();
		if (request.preferences() != null) {
			for (RequirementPreferenceDto dto : request.preferences()) {
				prefs.put(dto.requirementId(), new RequirementPreference(Set.copyOf(orEmpty(dto.requiredResourceIds())),
						Set.copyOf(orEmpty(dto.preferredResourceIds()))));
			}
		}
		return prefs;
	}

	private static List<AssignmentDto> assignments(CandidateOption option) {
		return option.assignments().stream()
				.map(a -> new AssignmentDto(UUID.fromString(a.requirementId()), UUID.fromString(a.resourceId())))
				.toList();
	}

	private static <T> List<T> orEmpty(List<T> list) {
		return list == null ? List.of() : list;
	}
}
