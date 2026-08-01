package com.allocra.app.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifies Flyway migrations apply against real PostgreSQL (PRD-NFR-005/006,
 * ADR-008) and that the baseline schema and the btree_gist extension required
 * for reservation integrity (ADR-004) are present. Requires Docker; runs in the
 * failsafe (integration-test) phase.
 */
@Testcontainers
class MigrationIT {

	@Container
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

	private static DataSource dataSource() {
		PGSimpleDataSource ds = new PGSimpleDataSource();
		ds.setUrl(POSTGRES.getJdbcUrl());
		ds.setUser(POSTGRES.getUsername());
		ds.setPassword(POSTGRES.getPassword());
		return ds;
	}

	@Test
	@DisplayName("PRD-NFR-005: Flyway migrations apply and create the baseline schema")
	void migrationsApplyAndCreateBaseline() throws Exception {
		// Deterministic filesystem location (failsafe working dir is the module base
		// dir),
		// so this test does not depend on classpath directory scanning in the forked
		// JVM.
		Flyway flyway = Flyway.configure().dataSource(dataSource())
				.locations("filesystem:src/main/resources/db/migration").load();

		var result = flyway.migrate();
		assertTrue(result.migrationsExecuted >= 1, "expected at least one migration to run");

		try (Connection c = dataSource().getConnection()) {
			try (ResultSet rs = c.createStatement()
					.executeQuery("SELECT to_regclass('public.tenant') IS NOT NULL AS present")) {
				rs.next();
				assertTrue(rs.getBoolean("present"), "tenant table should exist after migration");
			}
			try (ResultSet rs = c.createStatement()
					.executeQuery("SELECT count(*) AS n FROM pg_extension WHERE extname = 'btree_gist'")) {
				rs.next();
				assertEquals(1, rs.getInt("n"), "btree_gist extension should be enabled (required by ADR-004)");
			}
		}
	}
}
