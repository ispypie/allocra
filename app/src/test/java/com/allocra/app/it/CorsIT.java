package com.allocra.app.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.allocra.app.AllocraApplication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Verifies CORS enables a browser client from a configured origin and rejects
 * others, and that preflight is answered ahead of authentication (PRD-NFR-010 /
 * NFR-AT-001, ADR-011).
 */
class CorsIT {

	@SuppressWarnings("resource")
	private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

	private static final String ALLOWED = "http://localhost:3000";
	private static ConfigurableApplicationContext ctx;
	private static int port;
	private static final HttpClient HTTP = HttpClient.newHttpClient();

	@BeforeAll
	static void boot() {
		POSTGRES.start();
		ctx = new SpringApplicationBuilder(AllocraApplication.class).run("--server.port=0",
				"--allocra.cors.allowed-origins=" + ALLOWED, "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
				"--spring.datasource.username=" + POSTGRES.getUsername(),
				"--spring.datasource.password=" + POSTGRES.getPassword());
		port = ((ServletWebServerApplicationContext) ctx).getWebServer().getPort();
	}

	@AfterAll
	static void shutdown() {
		if (ctx != null) {
			ctx.close();
		}
		POSTGRES.stop();
	}

	@Test
	@DisplayName("PRD-NFR-010 / NFR-AT-001: a preflight from an allowed origin is accepted ahead of auth")
	void preflightFromAllowedOriginAccepted() throws Exception {
		HttpResponse<String> response = preflight(ALLOWED);
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).contains(ALLOWED);
	}

	@Test
	@DisplayName("PRD-NFR-010: a preflight from a non-allowed origin is rejected")
	void preflightFromDisallowedOriginRejected() throws Exception {
		HttpResponse<String> response = preflight("http://evil.example.com");
		assertThat(response.statusCode()).isEqualTo(403);
	}

	private static HttpResponse<String> preflight(String origin) throws Exception {
		HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v1/bookings"))
				.header("Origin", origin).header("Access-Control-Request-Method", "POST")
				.method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build();
		return HTTP.send(req, HttpResponse.BodyHandlers.ofString());
	}
}
