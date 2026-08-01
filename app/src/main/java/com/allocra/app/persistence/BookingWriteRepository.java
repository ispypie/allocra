package com.allocra.app.persistence;

import com.allocra.bookings.Booking;
import com.allocra.bookings.ResourceAssignment;
import com.allocra.common.error.ReservationConflictException;
import com.allocra.common.tenant.TenantId;
import com.allocra.reservations.Reservation;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Tenant-scoped writes for booking confirmation (ADR-003, ADR-004). All inserts
 * run inside the caller's transaction; a reservation that would overlap an
 * existing ACTIVE reservation trips the exclusion constraint, surfaced here as
 * {@link ReservationConflictException} → HTTP 409 (PRD-BKG-006 / PRD-RSV-004).
 */
@Repository
public class BookingWriteRepository {

	private final JdbcClient jdbc;

	public BookingWriteRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public void insertBooking(Booking booking) {
		jdbc.sql(
				"""
						INSERT INTO booking(tenant_id, id, service_type_id, subject_type, subject_display_name,
						                    subject_email, subject_phone, subject_external_ref, start_at, end_at, status, channel)
						VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
						""")
				.param(booking.tenantId().value()).param(booking.id()).param(booking.serviceTypeId())
				.param(booking.subject().type()).param(booking.subject().displayName()).param(booking.subject().email())
				.param(booking.subject().phone()).param(booking.subject().externalRef()).param(at(booking.startAt()))
				.param(at(booking.endAt())).param(booking.status().name()).param(booking.channel().name()).update();
	}

	public void insertAssignment(TenantId tenantId, ResourceAssignment assignment) {
		jdbc.sql("""
				INSERT INTO resource_assignment(tenant_id, id, booking_id, requirement_id, resource_id, policy)
				VALUES (?,?,?,?,?,?)
				""").param(tenantId.value()).param(assignment.id()).param(assignment.bookingId())
				.param(assignment.requirementId()).param(assignment.resourceId()).param(assignment.policy().name())
				.update();
	}

	/**
	 * @throws ReservationConflictException
	 *             if the resource is already reserved for an overlapping time
	 *             (exclusion constraint violation) — the transaction must roll
	 *             back.
	 */
	public void insertReservation(Reservation reservation) {
		try {
			jdbc.sql("""
					INSERT INTO reservation(tenant_id, id, booking_id, resource_id, start_at, end_at, status)
					VALUES (?,?,?,?,?,?,?)
					""").param(reservation.tenantId().value()).param(reservation.id()).param(reservation.bookingId())
					.param(reservation.resourceId()).param(at(reservation.startAt())).param(at(reservation.endAt()))
					.param(reservation.status().name()).update();
		} catch (DataIntegrityViolationException e) {
			throw new ReservationConflictException(
					"resource " + reservation.resourceId() + " is no longer available for the requested time", e);
		}
	}

	public void insertAudit(TenantId tenantId, String actor, String action, String targetType, UUID targetId) {
		jdbc.sql("""
				INSERT INTO audit_event(tenant_id, id, actor, action, target_type, target_id)
				VALUES (?,?,?,?,?,?)
				""").param(tenantId.value()).param(UUID.randomUUID()).param(actor).param(action).param(targetType)
				.param(targetId).update();
	}

	/** Current status of a booking, if it exists (tenant-scoped). */
	public Optional<String> findStatus(TenantId tenantId, UUID bookingId) {
		return jdbc.sql("SELECT status FROM booking WHERE tenant_id = ? AND id = ?").param(tenantId.value())
				.param(bookingId).query(String.class).optional();
	}

	/**
	 * Transitions a booking from {@code fromStatus} to {@code toStatus} only if it
	 * is still in {@code fromStatus} (guarding concurrent/invalid transitions).
	 *
	 * @return the number of rows updated (0 means the booking was not in
	 *         {@code fromStatus})
	 */
	public int updateStatus(TenantId tenantId, UUID bookingId, String fromStatus, String toStatus) {
		return jdbc.sql("UPDATE booking SET status = ? WHERE tenant_id = ? AND id = ? AND status = ?").param(toStatus)
				.param(tenantId.value()).param(bookingId).param(fromStatus).update();
	}

	/**
	 * Releases a booking's ACTIVE reservations so the freed slot can be rebooked
	 * (PRD-BKG-008).
	 */
	public void releaseReservations(TenantId tenantId, UUID bookingId) {
		jdbc.sql(
				"UPDATE reservation SET status = 'RELEASED' WHERE tenant_id = ? AND booking_id = ? AND status = 'ACTIVE'")
				.param(tenantId.value()).param(bookingId).update();
	}

	/**
	 * Moves a booking to a new time window (reschedule; the booking id is
	 * unchanged, PRD-ASN-002).
	 */
	public void updateSlot(TenantId tenantId, UUID bookingId, Instant startAt, Instant endAt) {
		jdbc.sql("UPDATE booking SET start_at = ?, end_at = ? WHERE tenant_id = ? AND id = ?").param(at(startAt))
				.param(at(endAt)).param(tenantId.value()).param(bookingId).update();
	}

	private static OffsetDateTime at(Instant instant) {
		return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
	}
}
