package com.allocra.app.persistence;

import com.allocra.common.tenant.TenantId;
import com.allocra.scheduling.BaseKind;
import com.allocra.scheduling.CapabilitySpec;
import com.allocra.scheduling.Interval;
import com.allocra.scheduling.ResourceCandidate;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
 * snapshot inputs).
 *
 * <p>
 * Each resource's <em>effective</em> availability is computed here as: the
 * resource's weekly availability rules ∩ its location's operating hours
 * (PRD-AVL-005), with the location's closure days removed (PRD-AVL-006), all
 * interpreted in the location's timezone (PRD-AVL-007, DST-aware). Blocks and
 * existing reservations are passed through and subtracted by the engine.
 * Folding business hours in here keeps the scheduling engine unchanged
 * (DEC-028).
 *
 * <p>
 * A resource with no location, or a location with no operating hours, is not
 * constrained by business hours (its own availability rules apply, interpreted
 * in the location timezone or UTC).
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
				SELECT r.id, r.resource_type_id, r.location_id, r.setup_minutes, r.cleanup_minutes
				FROM resource r
				JOIN resource_type t ON t.tenant_id = r.tenant_id AND t.id = r.resource_type_id
				WHERE r.tenant_id = ? AND t.base_kind = ? AND r.active = true
				ORDER BY r.id
				""").param(tenant).param(kindName)
				.query((rs, n) -> new Acc(rs.getObject("id", UUID.class), kind,
						rs.getObject("resource_type_id", UUID.class), rs.getObject("location_id", UUID.class),
						rs.getInt("setup_minutes"), rs.getInt("cleanup_minutes")))
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

		jdbc.sql("""
				SELECT ar.resource_id, ar.day_of_week, ar.start_time, ar.end_time
				FROM availability_rule ar
				JOIN resource r ON r.tenant_id = ar.tenant_id AND r.id = ar.resource_id
				JOIN resource_type t ON t.tenant_id = r.tenant_id AND t.id = r.resource_type_id
				WHERE ar.tenant_id = ? AND t.base_kind = ?
				""").param(tenant).param(kindName).query((rs, n) -> {
			Acc a = accs.get(rs.getObject("resource_id", UUID.class));
			if (a != null) {
				a.rules.add(new DayRange(rs.getInt("day_of_week"), rs.getObject("start_time", LocalTime.class),
						rs.getObject("end_time", LocalTime.class)));
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

		LocationData locations = loadLocationData(tenant);

		List<ResourceCandidate> candidates = new ArrayList<>();
		for (Acc a : accs.values()) {
			ZoneId zone = locations.zoneOf(a.locationId);
			List<DayRange> hours = locations.operatingHoursOf(a.locationId); // null => unconstrained
			List<ClosureRange> closures = locations.closuresOf(a.locationId);
			List<Interval> availability = effectiveAvailability(a.rules, hours, closures, zone, window);
			candidates.add(new ResourceCandidate(a.id.toString(), a.kind, a.resourceTypeId.toString(), a.capabilities,
					availability, a.blocked, a.reservations, a.compatible, a.setupMinutes, a.cleanupMinutes));
		}
		return candidates;
	}

	/**
	 * Effective availability windows (UTC), computed per day across the window:
	 * resource rules intersected with location operating hours (when defined),
	 * excluding closure days, all in the location's timezone.
	 */
	private static List<Interval> effectiveAvailability(List<DayRange> rules, List<DayRange> operatingHours,
			List<ClosureRange> closures, ZoneId zone, Interval window) {
		List<Interval> result = new ArrayList<>();
		boolean hoursDefined = operatingHours != null && !operatingHours.isEmpty();
		LocalDate from = window.start().atZone(zone).toLocalDate();
		LocalDate to = window.end().atZone(zone).toLocalDate();

		for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
			if (isClosed(date, closures)) {
				continue;
			}
			int dow = date.getDayOfWeek().getValue();
			List<DayRange> resourceRanges = rangesForDay(rules, dow);
			if (resourceRanges.isEmpty()) {
				continue; // resource does not work this day
			}
			if (hoursDefined) {
				List<DayRange> businessRanges = rangesForDay(operatingHours, dow);
				if (businessRanges.isEmpty()) {
					continue; // location closed this weekday
				}
				for (DayRange r : resourceRanges) {
					for (DayRange b : businessRanges) {
						LocalTime start = maxTime(r.start(), b.start());
						LocalTime end = minTime(r.end(), b.end());
						if (start.isBefore(end)) {
							result.add(toUtc(date, start, end, zone));
						}
					}
				}
			} else {
				for (DayRange r : resourceRanges) {
					result.add(toUtc(date, r.start(), r.end(), zone));
				}
			}
		}
		return result;
	}

	private static Interval toUtc(LocalDate date, LocalTime start, LocalTime end, ZoneId zone) {
		return new Interval(ZonedDateTime.of(date, start, zone).toInstant(),
				ZonedDateTime.of(date, end, zone).toInstant());
	}

	private static boolean isClosed(LocalDate date, List<ClosureRange> closures) {
		for (ClosureRange c : closures) {
			if (!date.isBefore(c.start()) && !date.isAfter(c.end())) {
				return true;
			}
		}
		return false;
	}

	private static List<DayRange> rangesForDay(List<DayRange> ranges, int dayOfWeek) {
		List<DayRange> out = new ArrayList<>();
		for (DayRange r : ranges) {
			if (r.dayOfWeek() == dayOfWeek) {
				out.add(r);
			}
		}
		return out;
	}

	private static LocalTime maxTime(LocalTime a, LocalTime b) {
		return a.isAfter(b) ? a : b;
	}

	private static LocalTime minTime(LocalTime a, LocalTime b) {
		return a.isBefore(b) ? a : b;
	}

	private LocationData loadLocationData(UUID tenant) {
		Map<UUID, ZoneId> zones = new HashMap<>();
		jdbc.sql("SELECT id, timezone FROM location WHERE tenant_id = ?").param(tenant)
				.query((rs, n) -> zones.put(rs.getObject("id", UUID.class), safeZone(rs.getString("timezone")))).list();

		Map<UUID, List<DayRange>> hours = new HashMap<>();
		jdbc.sql(
				"SELECT location_id, day_of_week, open_time, close_time FROM location_operating_hours WHERE tenant_id = ?")
				.param(tenant)
				.query((rs, n) -> hours.computeIfAbsent(rs.getObject("location_id", UUID.class), k -> new ArrayList<>())
						.add(new DayRange(rs.getInt("day_of_week"), rs.getObject("open_time", LocalTime.class),
								rs.getObject("close_time", LocalTime.class))))
				.list();

		Map<UUID, List<ClosureRange>> closures = new HashMap<>();
		jdbc.sql("SELECT location_id, start_date, end_date FROM location_closure WHERE tenant_id = ?").param(tenant)
				.query((rs, n) -> closures
						.computeIfAbsent(rs.getObject("location_id", UUID.class), k -> new ArrayList<>())
						.add(new ClosureRange(rs.getObject("start_date", LocalDate.class),
								rs.getObject("end_date", LocalDate.class))))
				.list();

		return new LocationData(zones, hours, closures);
	}

	private static ZoneId safeZone(String tz) {
		try {
			return tz == null ? ZoneOffset.UTC : ZoneId.of(tz);
		} catch (RuntimeException e) {
			return ZoneOffset.UTC;
		}
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

	private record DayRange(int dayOfWeek, LocalTime start, LocalTime end) {
	}

	private record ClosureRange(LocalDate start, LocalDate end) {
	}

	private record LocationData(Map<UUID, ZoneId> zones, Map<UUID, List<DayRange>> operatingHours,
			Map<UUID, List<ClosureRange>> closures) {

		ZoneId zoneOf(UUID locationId) {
			if (locationId == null) {
				return ZoneOffset.UTC;
			}
			return zones.getOrDefault(locationId, ZoneOffset.UTC);
		}

		List<DayRange> operatingHoursOf(UUID locationId) {
			return locationId == null ? null : operatingHours.get(locationId);
		}

		List<ClosureRange> closuresOf(UUID locationId) {
			return locationId == null ? List.of() : closures.getOrDefault(locationId, List.of());
		}
	}

	private static final class Acc {
		private final UUID id;
		private final BaseKind kind;
		private final UUID resourceTypeId;
		private final UUID locationId;
		private final int setupMinutes;
		private final int cleanupMinutes;
		private final List<CapabilitySpec> capabilities = new ArrayList<>();
		private final List<DayRange> rules = new ArrayList<>();
		private final List<Interval> blocked = new ArrayList<>();
		private final List<Interval> reservations = new ArrayList<>();
		private final Set<String> compatible = new LinkedHashSet<>();

		private Acc(UUID id, BaseKind kind, UUID resourceTypeId, UUID locationId, int setupMinutes,
				int cleanupMinutes) {
			this.id = id;
			this.kind = kind;
			this.resourceTypeId = resourceTypeId;
			this.locationId = locationId;
			this.setupMinutes = setupMinutes;
			this.cleanupMinutes = cleanupMinutes;
		}
	}
}
