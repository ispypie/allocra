package com.allocra.app.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifies, at the schema level against real PostgreSQL, that the reservation
 * exclusion constraint rejects overlapping ACTIVE reservations for the same
 * resource (ADR-004, PRD-RSV-004 / RSV-AT-001). This exercises the full FK
 * chain (tenant → service_type → booking, resource_type → resource →
 * reservation) and the generated {@code during} column.
 */
@Testcontainers
class ReservationSchemaIT {

	@Container
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

	private static final UUID TENANT = UUID.randomUUID();
	private static final UUID RESOURCE = UUID.randomUUID();
	private static final UUID BOOKING = UUID.randomUUID();
	private static final OffsetDateTime TEN = OffsetDateTime.of(2026, 9, 1, 10, 0, 0, 0, ZoneOffset.UTC);

	@BeforeAll
	static void migrateAndSeed() throws Exception {
		Flyway.configure().dataSource(dataSource()).locations("filesystem:src/main/resources/db/migration").load()
				.migrate();
		try (Connection c = dataSource().getConnection()) {
			exec(c, "INSERT INTO tenant(id, slug, display_name) VALUES (?, 'acme', 'Acme')", TENANT);
			UUID serviceType = UUID.randomUUID();
			exec(c, "INSERT INTO service_type(tenant_id, id, code, name, duration_minutes) VALUES (?,?,'TREAT','Treatment',60)",
					TENANT, serviceType);
			exec(c, "INSERT INTO booking(tenant_id, id, service_type_id, subject_type, subject_display_name, start_at, end_at, status, channel)"
					+ " VALUES (?,?,?, 'PERSON', 'Alex', ?, ?, 'CONFIRMED', 'INTERNAL')", TENANT, BOOKING, serviceType,
					TEN, TEN.plusHours(1));
			UUID resourceType = UUID.randomUUID();
			exec(c, "INSERT INTO resource_type(tenant_id, id, code, base_kind) VALUES (?,?,'ROOM','PLACE')", TENANT,
					resourceType);
			exec(c, "INSERT INTO resource(tenant_id, id, resource_type_id, name) VALUES (?,?,?, 'Room 1')", TENANT,
					RESOURCE, resourceType);
		}
	}

	@Test
	@DisplayName("PRD-RSV-004 / RSV-AT-001: overlapping ACTIVE reservations for the same resource are rejected")
	void overlappingReservationsRejected() throws Exception {
		insertReservation(TEN, TEN.plusHours(1)); // 10:00–11:00 succeeds

		assertThatThrownBy(() -> insertReservation(TEN.plusMinutes(30), TEN.plusMinutes(90))) // 10:30–11:30 overlaps
				.isInstanceOf(SQLException.class)
				.satisfies(e -> assertThat(((SQLException) e).getSQLState()).isEqualTo("23P01")); // exclusion_violation

		// A non-overlapping reservation for the same resource is accepted.
		insertReservation(TEN.plusHours(1), TEN.plusHours(2)); // 11:00–12:00
	}

	private void insertReservation(OffsetDateTime start, OffsetDateTime end) throws SQLException {
		try (Connection c = dataSource().getConnection();
				PreparedStatement ps = c.prepareStatement(
						"INSERT INTO reservation(tenant_id, id, booking_id, resource_id, start_at, end_at, status)"
								+ " VALUES (?,?,?,?,?,?, 'ACTIVE')")) {
			ps.setObject(1, TENANT);
			ps.setObject(2, UUID.randomUUID());
			ps.setObject(3, BOOKING);
			ps.setObject(4, RESOURCE);
			ps.setObject(5, start);
			ps.setObject(6, end);
			ps.executeUpdate();
		}
	}

	private static void exec(Connection c, String sql, Object... params) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			for (int i = 0; i < params.length; i++) {
				ps.setObject(i + 1, params[i]);
			}
			ps.executeUpdate();
		}
	}

	private static DataSource dataSource() {
		PGSimpleDataSource ds = new PGSimpleDataSource();
		ds.setUrl(POSTGRES.getJdbcUrl());
		ds.setUser(POSTGRES.getUsername());
		ds.setPassword(POSTGRES.getPassword());
		return ds;
	}
}
