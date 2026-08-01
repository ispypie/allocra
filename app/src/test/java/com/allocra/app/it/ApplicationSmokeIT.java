package com.allocra.app.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.allocra.app.AllocraApplication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Smoke test: the real application boots against real PostgreSQL (Flyway runs
 * at startup) and the liveness/readiness probes report healthy
 * (PRD-NFR-002/003). Requires Docker; runs in the failsafe phase.
 *
 * <p>
 * The app is started programmatically via {@link SpringApplicationBuilder}
 * rather than {@code @SpringBootTest}, to avoid the Spring TestContext
 * bootstrapper's classloader-identity issues in the forked failsafe JVM. This
 * exercises the same production wiring end to end.
 */
@Testcontainers
class ApplicationSmokeIT {

	@Container
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

	@Test
	@DisplayName("PRD-NFR-002/003: application boots on PostgreSQL and liveness/readiness report UP")
	void applicationBootsAndProbesAreHealthy() throws Exception {
		try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(AllocraApplication.class)
				.properties("server.port=0", "management.endpoint.health.probes.enabled=true",
						"spring.datasource.url=" + POSTGRES.getJdbcUrl(),
						"spring.datasource.username=" + POSTGRES.getUsername(),
						"spring.datasource.password=" + POSTGRES.getPassword())
				.run()) {

			int port = ((ServletWebServerApplicationContext) ctx).getWebServer().getPort();
			HttpClient client = HttpClient.newHttpClient();

			for (String probe : new String[]{"liveness", "readiness"}) {
				HttpResponse<String> response = client.send(HttpRequest
						.newBuilder(URI.create("http://localhost:" + port + "/actuator/health/" + probe)).build(),
						HttpResponse.BodyHandlers.ofString());
				assertThat(response.statusCode()).isEqualTo(200);
				assertThat(response.body()).contains("UP");
			}
		}
	}
}
