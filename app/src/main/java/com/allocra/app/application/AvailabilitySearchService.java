package com.allocra.app.application;

import com.allocra.app.persistence.CatalogRepository;
import com.allocra.app.persistence.CatalogRepository.ServiceDefinition;
import com.allocra.common.error.ResourceNotFoundException;
import com.allocra.common.tenant.TenantId;
import com.allocra.scheduling.CandidateOption;
import com.allocra.scheduling.DirectAvailabilitySearch;
import com.allocra.scheduling.Interval;
import com.allocra.scheduling.SchedulingSnapshot;
import com.allocra.scheduling.SearchParameters;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Direct availability search (PRD-SCH-004). Advisory and read-only: it returns
 * bookable options and never changes any booking, assignment or reservation
 * (PRD-ASN-004). The window is clamped to the default search horizon
 * (OQ-SCH-1).
 */
@Service
public class AvailabilitySearchService {

	private static final Duration HORIZON = Duration.ofDays(14);

	private final CatalogRepository catalog;
	private final SchedulingSnapshotBuilder snapshots;
	private final DirectAvailabilitySearch engine = new DirectAvailabilitySearch();

	public AvailabilitySearchService(CatalogRepository catalog, SchedulingSnapshotBuilder snapshots) {
		this.catalog = catalog;
		this.snapshots = snapshots;
	}

	public List<CandidateOption> search(TenantId tenantId, UUID serviceTypeId, Instant from, Instant to,
			Map<UUID, SchedulingSnapshotBuilder.RequirementPreference> preferences) {
		ServiceDefinition service = catalog.findService(tenantId, serviceTypeId)
				.orElseThrow(() -> new ResourceNotFoundException("service " + serviceTypeId + " not found"));
		Instant clampedEnd = to.isAfter(from.plus(HORIZON)) ? from.plus(HORIZON) : to;
		if (!clampedEnd.isAfter(from)) {
			throw new IllegalArgumentException("search window end must be after start");
		}
		Interval window = new Interval(from, clampedEnd);
		SchedulingSnapshot snapshot = snapshots.forSearch(tenantId, service, window, SearchParameters.defaults(),
				preferences);
		return engine.search(snapshot);
	}
}
