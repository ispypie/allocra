package com.allocra.app.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.allocra.app.AllocraApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Smoke test: the full Spring context loads against real PostgreSQL (via
 * Testcontainers {@code @ServiceConnection}), migrations run at startup, and
 * the readiness/liveness probes report healthy (PRD-NFR-002/003). Requires
 * Docker; runs in the failsafe phase.
 */
@SpringBootTest(classes = AllocraApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ApplicationSmokeIT {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired
	private TestRestTemplate rest;

	@Test
	@DisplayName("PRD-NFR-003: liveness and readiness probes report UP")
	void probesReportHealthy() {
		ResponseEntity<String> liveness = rest.getForEntity("/actuator/health/liveness", String.class);
		assertThat(liveness.getStatusCode().is2xxSuccessful()).isTrue();
		assertThat(liveness.getBody()).contains("UP");

		ResponseEntity<String> readiness = rest.getForEntity("/actuator/health/readiness", String.class);
		assertThat(readiness.getStatusCode().is2xxSuccessful()).isTrue();
		assertThat(readiness.getBody()).contains("UP");
	}
}
