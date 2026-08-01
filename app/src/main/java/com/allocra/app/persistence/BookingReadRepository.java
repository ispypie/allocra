package com.allocra.app.persistence;

import com.allocra.common.tenant.TenantId;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Tenant-scoped reads of bookings and their assignments (PRD-BKG-010/011). */
@Repository
public class BookingReadRepository {

	private final JdbcClient jdbc;

	public BookingReadRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public Optional<BookingView> findById(TenantId tenantId, UUID bookingId) {
		Optional<BookingView> booking = jdbc.sql("""
				SELECT id, service_type_id, subject_type, subject_display_name, subject_email, subject_phone,
				       subject_external_ref, start_at, end_at, status, channel
				FROM booking WHERE tenant_id = ? AND id = ?
				""").param(tenantId.value()).param(bookingId)
				.query((rs, n) -> new BookingView(rs.getObject("id", UUID.class),
						rs.getObject("service_type_id", UUID.class), rs.getString("subject_type"),
						rs.getString("subject_display_name"), rs.getString("subject_email"),
						rs.getString("subject_phone"), rs.getString("subject_external_ref"),
						rs.getObject("start_at", OffsetDateTime.class).toInstant(),
						rs.getObject("end_at", OffsetDateTime.class).toInstant(), rs.getString("status"),
						rs.getString("channel"), new ArrayList<>()))
				.optional();
		booking.ifPresent(b -> b.assignments().addAll(loadAssignments(tenantId, bookingId)));
		return booking;
	}

	/**
	 * Current assignments of a booking as requirementId → resourceId (used by
	 * reschedule).
	 */
	public Map<UUID, UUID> findAssignmentMap(TenantId tenantId, UUID bookingId) {
		Map<UUID, UUID> map = new LinkedHashMap<>();
		loadAssignments(tenantId, bookingId).forEach(a -> map.put(a.requirementId(), a.resourceId()));
		return map;
	}

	public List<BookingSummary> list(TenantId tenantId, Optional<String> status) {
		StringBuilder sql = new StringBuilder("""
				SELECT id, service_type_id, subject_display_name, start_at, end_at, status, channel
				FROM booking WHERE tenant_id = ?
				""");
		status.ifPresent(s -> sql.append(" AND status = ?"));
		sql.append(" ORDER BY start_at");
		var spec = jdbc.sql(sql.toString()).param(tenantId.value());
		if (status.isPresent()) {
			spec = spec.param(status.get());
		}
		return spec.query((rs, n) -> new BookingSummary(rs.getObject("id", UUID.class),
				rs.getObject("service_type_id", UUID.class), rs.getString("subject_display_name"),
				rs.getObject("start_at", OffsetDateTime.class).toInstant(),
				rs.getObject("end_at", OffsetDateTime.class).toInstant(), rs.getString("status"),
				rs.getString("channel"))).list();
	}

	private List<AssignmentView> loadAssignments(TenantId tenantId, UUID bookingId) {
		return jdbc.sql(
				"SELECT requirement_id, resource_id, policy FROM resource_assignment WHERE tenant_id = ? AND booking_id = ?")
				.param(tenantId.value()).param(bookingId)
				.query((rs, n) -> new AssignmentView(rs.getObject("requirement_id", UUID.class),
						rs.getObject("resource_id", UUID.class), rs.getString("policy")))
				.list();
	}

	public record BookingView(UUID id, UUID serviceTypeId, String subjectType, String subjectDisplayName,
			String subjectEmail, String subjectPhone, String subjectExternalRef, Instant startAt, Instant endAt,
			String status, String channel, List<AssignmentView> assignments) {
	}

	public record AssignmentView(UUID requirementId, UUID resourceId, String policy) {
	}

	public record BookingSummary(UUID id, UUID serviceTypeId, String subjectDisplayName, Instant startAt, Instant endAt,
			String status, String channel) {
	}
}
