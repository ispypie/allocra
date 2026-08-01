package com.allocra.app.persistence;

import com.allocra.common.tenant.TenantId;
import com.allocra.scheduling.BaseKind;
import com.allocra.scheduling.CapabilitySpec;
import com.allocra.scheduling.Interval;
import com.allocra.scheduling.ResourceCandidate;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Loads {@link ResourceCandidate}s (the pure engine's input) for all active
 * resources of a given base kind within a tenant and window (PRD-SCH-001
 * snapshot inputs). Weekly availability rules (OQ-AVL-1) are expanded into
 * concrete UTC intervals across the window's days; times are treated as UTC for
 * the slice (OQ-TIME-1).
 */
@Repository
public class CandidateRepository {

	private final JdbcClient jdbc;

	public CandidateRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public List<ResourceCandidate> loadCandidates(TenantId tenantId, BaseKind kind, Interval window) {
		UUID tenant = tenantId.value();
		String kindName = kind.name();
		Map<UUID, Acc> accs = new LinkedHashMap<>();

		jdbc.sql("""
				SELECT r.id, r.resource_type_id
				FROM resource r
				JOIN resource_type t ON t.tenant_id = r.tenant_id AND t.id = r.resource_type_id
				WHERE r.tenant_id = ? AND t.base_kind = ? AND r.active = true
				ORDER BY r.id
				""").param(tenant).param(kindName).query(
				(rs, n) -> new Acc(rs.getObject("id", UUID.class), kind, rs.getObject("resource_type_id", UUID.class)))
				.list().forEach(a -> accs.put(a.id, a));

		if (accs.isEmpty()) {
			return List.of();
		}

		jdbc.sql("""
				SELECT c.resource_id, c.capability_type, c.level, c.valid_from, c.valid_to
				FROM resource_capability c
				JOIN resource r ON r.tenant_id = c.tenant_id AND r.id = c.resource_id
				JOIN resource_type t ON t.tenant_id = r.tenant_id AND t.id = r.resource_type_id
				WHERE c.tenant_id = ? AND t.base_kind = ?
				""").param(tenant).param(kindName).query((rs, n) -> {
			Acc a = accs.get(rs.getObject("resource_id", UUID.class));
			if (a != null) {
				a.capabilities.add(new CapabilitySpec(rs.getString("capability_type"), (Integer) rs.getObject("level"),
						rs.getObject("valid_from", LocalDate.class), rs.getObject("valid_to", LocalDate.class)));
			}
			return null;
		}).list();

		LocalDate from = window.start().atZone(ZoneOffset.UTC).toLocalDate();
		LocalDate to = window.end().atZone(ZoneOffset.UTC).toLocalDate();
		jdbc.sql("""
				SELECT ar.resource_id, ar.day_of_week, ar.start_time, ar.end_time
				FROM availability_rule ar
				JOIN resource r ON r.tenant_id = ar.tenant_id AND r.id = ar.resource_id
				JOIN resource_type t ON t.tenant_id = r.tenant_id AND t.id = r.resource_type_id
				WHERE ar.tenant_id = ? AND t.base_kind = ?
				""").param(tenant).param(kindName).query((rs, n) -> {
			Acc a = accs.get(rs.getObject("resource_id", UUID.class));
			if (a != null) {
				expandRule(a, rs.getInt("day_of_week"), rs.getObject("start_time", LocalTime.class),
						rs.getObject("end_time", LocalTime.class), from, to);
			}
			return null;
		}).list();

		loadWindows(tenant, kindName, window, "blocked_availability", accs, false);
		loadWindows(tenant, kindName, window, "reservation", accs, true);

		jdbc.sql("""
				SELECT rc.resource_id, rc.compatible_resource_id
				FROM resource_compatibility rc
				JOIN resource r ON r.tenant_id = rc.tenant_id AND r.id = rc.resource_id
				JOIN resource_type t ON t.tenant_id = r.tenant_id AND t.id = r.resource_type_id
				WHERE rc.tenant_id = ? AND t.base_kind = ?
				""").param(tenant).param(kindName).query((rs, n) -> {
			Acc a = accs.get(rs.getObject("resource_id", UUID.class));
			if (a != null) {
				a.compatible.add(rs.getObject("compatible_resource_id", UUID.class).toString());
			}
			return null;
		}).list();

		List<ResourceCandidate> candidates = new ArrayList<>();
		for (Acc a : accs.values()) {
			candidates.add(a.toCandidate());
		}
		return candidates;
	}

	private void loadWindows(UUID tenant, String kindName, Interval window, String table, Map<UUID, Acc> accs,
			boolean activeOnly) {
		String sql = "SELECT w.resource_id, w.start_at, w.end_at FROM " + table + " w "
				+ "JOIN resource r ON r.tenant_id = w.tenant_id AND r.id = w.resource_id "
				+ "JOIN resource_type t ON t.tenant_id = r.tenant_id AND t.id = r.resource_type_id "
				+ "WHERE w.tenant_id = ? AND t.base_kind = ? AND w.start_at < ? AND w.end_at > ?"
				+ (activeOnly ? " AND w.status = 'ACTIVE'" : "");
		jdbc.sql(sql).param(tenant).param(kindName).param(OffsetDateTime.ofInstant(window.end(), ZoneOffset.UTC))
				.param(OffsetDateTime.ofInstant(window.start(), ZoneOffset.UTC)).query((rs, n) -> {
					Acc a = accs.get(rs.getObject("resource_id", UUID.class));
					if (a != null) {
						Interval iv = new Interval(rs.getObject("start_at", OffsetDateTime.class).toInstant(),
								rs.getObject("end_at", OffsetDateTime.class).toInstant());
						if (activeOnly) {
							a.reservations.add(iv);
						} else {
							a.blocked.add(iv);
						}
					}
					return null;
				}).list();
	}

	private static void expandRule(Acc a, int dayOfWeek, LocalTime start, LocalTime end, LocalDate from, LocalDate to) {
		for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
			if (d.getDayOfWeek().getValue() == dayOfWeek) {
				a.availability.add(new Interval(d.atTime(start).toInstant(ZoneOffset.UTC),
						d.atTime(end).toInstant(ZoneOffset.UTC)));
			}
		}
	}

	private static final class Acc {
		private final UUID id;
		private final BaseKind kind;
		private final UUID resourceTypeId;
		private final List<CapabilitySpec> capabilities = new ArrayList<>();
		private final List<Interval> availability = new ArrayList<>();
		private final List<Interval> blocked = new ArrayList<>();
		private final List<Interval> reservations = new ArrayList<>();
		private final Set<String> compatible = new LinkedHashSet<>();

		private Acc(UUID id, BaseKind kind, UUID resourceTypeId) {
			this.id = id;
			this.kind = kind;
			this.resourceTypeId = resourceTypeId;
		}

		private ResourceCandidate toCandidate() {
			return new ResourceCandidate(id.toString(), kind, resourceTypeId.toString(), capabilities, availability,
					blocked, reservations, compatible);
		}
	}
}
