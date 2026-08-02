package com.allocra.app.application;

import com.allocra.app.persistence.CandidateRepository;
import com.allocra.app.persistence.CatalogRepository.RequirementRow;
import com.allocra.app.persistence.CatalogRepository.ServiceDefinition;
import com.allocra.common.tenant.TenantId;
import com.allocra.scheduling.BaseKind;
import com.allocra.scheduling.Interval;
import com.allocra.scheduling.RequirementSpec;
import com.allocra.scheduling.ResourceCandidate;
import com.allocra.scheduling.SchedulingSnapshot;
import com.allocra.scheduling.SearchParameters;
import com.allocra.scheduling.SelectionMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Builds an immutable {@link SchedulingSnapshot} from tenant-scoped
 * repositories (PRD-SCH-001). The pure engine consumes the snapshot; this
 * component is the only bridge from persistence to the engine's input contract.
 */
@Component
public class SchedulingSnapshotBuilder {

	private final CandidateRepository candidates;

	public SchedulingSnapshotBuilder(CandidateRepository candidates) {
		this.candidates = candidates;
	}

	/**
	 * Snapshot for direct availability search across a window, honouring request
	 * preferences.
	 */
	public SchedulingSnapshot forSearch(TenantId tenantId, ServiceDefinition service, Interval window,
			SearchParameters parameters, Map<UUID, RequirementPreference> preferences) {
		Map<BaseKind, List<ResourceCandidate>> byKind = loadByKind(tenantId, service, window);
		List<RequirementSpec> requirements = new ArrayList<>();
		Map<String, List<ResourceCandidate>> candidatesByRequirement = new java.util.HashMap<>();
		for (RequirementRow row : service.requirements()) {
			RequirementPreference pref = preferences.getOrDefault(row.id(), RequirementPreference.none());
			requirements.add(toSpec(row, pref.requiredResourceIds(), pref.preferredResourceIds()));
			candidatesByRequirement.put(row.id().toString(), byKind.getOrDefault(row.kind(), List.of()));
		}
		return new SchedulingSnapshot(window, durationOf(service), requirements, candidatesByRequirement, parameters,
				service.leadMinutes());
	}

	/**
	 * Snapshot for confirmation re-validation: a single slot, with each requirement
	 * restricted to the specifically chosen resource so the engine confirms that
	 * exact combination is feasible.
	 */
	public SchedulingSnapshot forConfirm(TenantId tenantId, ServiceDefinition service, Interval slot,
			Map<UUID, UUID> chosen) {
		Map<BaseKind, List<ResourceCandidate>> byKind = loadByKind(tenantId, service, slot);
		List<RequirementSpec> requirements = new ArrayList<>();
		Map<String, List<ResourceCandidate>> candidatesByRequirement = new java.util.HashMap<>();
		for (RequirementRow row : service.requirements()) {
			UUID chosenResource = chosen.get(row.id());
			Set<UUID> required = chosenResource == null ? Set.of() : Set.of(chosenResource);
			requirements.add(toSpec(row, required, Set.of()));
			List<ResourceCandidate> pool = byKind.getOrDefault(row.kind(), List.of()).stream()
					.filter(c -> chosenResource != null && c.resourceId().equals(chosenResource.toString()))
					.collect(Collectors.toList());
			candidatesByRequirement.put(row.id().toString(), pool);
		}
		SearchParameters oneSlot = new SearchParameters(Duration.between(slot.start(), slot.end()), 1);
		return new SchedulingSnapshot(slot, durationOf(service), requirements, candidatesByRequirement, oneSlot,
				service.leadMinutes());
	}

	private Map<BaseKind, List<ResourceCandidate>> loadByKind(TenantId tenantId, ServiceDefinition service,
			Interval window) {
		Map<BaseKind, List<ResourceCandidate>> byKind = new EnumMap<>(BaseKind.class);
		for (RequirementRow row : service.requirements()) {
			byKind.computeIfAbsent(row.kind(), k -> candidates.loadCandidates(tenantId, k, window));
		}
		return byKind;
	}

	private RequirementSpec toSpec(RequirementRow row, Set<UUID> requiredIds, Set<UUID> preferredIds) {
		SelectionMode mode = requiredIds.isEmpty()
				? SelectionMode.valueOf(row.selectionMode())
				: SelectionMode.REQUIRED;
		return new RequirementSpec(row.id().toString(), row.kind(), row.required(), mode, row.capability(),
				requiredIds.stream().map(UUID::toString).collect(Collectors.toSet()),
				preferredIds.stream().map(UUID::toString).collect(Collectors.toSet()));
	}

	private static Duration durationOf(ServiceDefinition service) {
		return Duration.ofMinutes(service.durationMinutes());
	}

	/**
	 * Per-requirement resource preferences supplied by the booking request
	 * (PRD-SVC-005).
	 */
	public record RequirementPreference(Set<UUID> requiredResourceIds, Set<UUID> preferredResourceIds) {
		public RequirementPreference {
			requiredResourceIds = Set.copyOf(requiredResourceIds);
			preferredResourceIds = Set.copyOf(preferredResourceIds);
		}

		public static RequirementPreference none() {
			return new RequirementPreference(Set.of(), Set.of());
		}
	}
}
