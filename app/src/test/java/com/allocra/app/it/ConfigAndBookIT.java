package com.allocra.app.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.allocra.app.AllocraApplication;
import com.allocra.app.web.ApiModel.AssignmentDto;
import com.allocra.app.web.ApiModel.ConfirmRequest;
import com.allocra.app.web.ApiModel.SearchRequest;
import com.allocra.app.web.ApiModel.SearchResponse;
import com.allocra.app.web.ApiModel.SubjectDto;
import com.allocra.app.web.ConfigApi.CapabilitySpecDto;
import com.allocra.app.web.ConfigApi.CreateAvailabilityRuleRequest;
import com.allocra.app.web.ConfigApi.CreateResourceRequest;
import com.allocra.app.web.ConfigApi.CreateResourceTypeRequest;
import com.allocra.app.web.ConfigApi.CreateServiceRequest;
import com.allocra.app.web.ConfigApi.CreatedServiceResponse;
import com.allocra.app.web.ConfigApi.IdResponse;
import com.allocra.app.web.ConfigApi.RequirementSpecDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Configures a bookable service entirely over the API (no SQL beyond
 * bootstrapping a tenant and an admin/viewer) and then books it — the
 * config→book loop (PRD-RES-010, PRD-SVC-008, PRD-AVL-004). Also checks a
 * non-privileged user cannot configure.
 */
class ConfigAndBookIT {

	@SuppressWarnings("resource")
	private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

	private static ConfigurableApplicationContext ctx;
	private static ObjectMapper json;
	private static int port;
	private static final HttpClient HTTP = HttpClient.newHttpClient();

	private static final UUID TENANT = UUID.randomUUID();
	private static final LocalDate DATE = LocalDate.of(2026, 9, 7);

	@BeforeAll
	static void boot() {
		POSTGRES.start();
		ctx = new SpringApplicationBuilder(AllocraApplication.class).run("--server.port=0",
				"--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
				"--spring.datasource.username=" + POSTGRES.getUsername(),
				"--spring.datasource.password=" + POSTGRES.getPassword());
		port = ((ServletWebServerApplicationContext) ctx).getWebServer().getPort();
		json = ctx.getBean(ObjectMapper.class);
		JdbcClient jdbc = ctx.getBean(JdbcClient.class);

		// Bootstrap: a tenant, an ORG_ADMIN (can configure) and a VIEWER (cannot).
		jdbc.sql("INSERT INTO tenant(id,slug,display_name) VALUES(?,?,?)").params(TENANT, "acme", "Acme").update();
		seedMember(jdbc, "admin", "ORG_ADMIN");
		seedMember(jdbc, "viewer", "VIEWER");
	}

	@AfterAll
	static void shutdown() {
		if (ctx != null) {
			ctx.close();
		}
		POSTGRES.stop();
	}

	private static void seedMember(JdbcClient jdbc, String firebaseUid, String role) {
		UUID userId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		jdbc.sql("INSERT INTO application_user(id,firebase_uid,display_name) VALUES(?,?,?)")
				.params(userId, firebaseUid, firebaseUid).update();
		jdbc.sql("INSERT INTO organisation_member(tenant_id,id,user_id,status) VALUES(?,?,?,?)")
				.params(TENANT, memberId, userId, "ACTIVE").update();
		jdbc.sql("INSERT INTO organisation_member_role(tenant_id,member_id,role) VALUES(?,?,?)")
				.params(TENANT, memberId, role).update();
	}

	@Test
	@DisplayName("PRD-RES-010 / PRD-SVC-008 / PRD-AVL-004 (RES-AT-006 / SVC-AT-002): configure a service over the API, then book it")
	void configureThenBook() throws Exception {
		// A viewer cannot configure.
		assertThat(post("/v1/resource-types", "viewer",
				json.writeValueAsString(new CreateResourceTypeRequest("X", "PERSON"))).statusCode()).isEqualTo(403);

		UUID staffType = created(post("/v1/resource-types", "admin",
				json.writeValueAsString(new CreateResourceTypeRequest("STAFF", "PERSON"))));
		UUID roomType = created(post("/v1/resource-types", "admin",
				json.writeValueAsString(new CreateResourceTypeRequest("ROOM", "PLACE"))));

		UUID staff = created(post("/v1/resources", "admin", json.writeValueAsString(new CreateResourceRequest(staffType,
				"Sam Physio", null, null, List.of(new CapabilitySpecDto("PHYSIO", null, null, null)), null, 0, 0))));
		UUID room = created(post("/v1/resources", "admin",
				json.writeValueAsString(new CreateResourceRequest(roomType, "Room 1", null, null, null, null, 0, 0))));

		int dow = DATE.getDayOfWeek().getValue();
		for (UUID r : List.of(staff, room)) {
			assertThat(post("/v1/resources/" + r + "/availability-rules", "admin",
					json.writeValueAsString(
							new CreateAvailabilityRuleRequest(dow, LocalTime.of(8, 0), LocalTime.of(18, 0))))
					.statusCode()).isEqualTo(201);
		}

		CreatedServiceResponse service = json.readValue(
				post("/v1/services", "admin",
						json.writeValueAsString(new CreateServiceRequest("TREAT", "Treatment", 60, 0,
								List.of(new RequirementSpecDto("PERSON", true, "ANY", "PHYSIO", null),
										new RequirementSpecDto("PLACE", true, "ANY", null, null)))))
						.body(),
				CreatedServiceResponse.class);
		UUID staffReq = requirementOf(service, "PERSON");
		UUID roomReq = requirementOf(service, "PLACE");

		// Search finds the configured service, then a booking is confirmed.
		SearchResponse search = json.readValue(
				post("/v1/services/" + service.serviceTypeId() + "/availability/search", "admin",
						json.writeValueAsString(new SearchRequest(at(9, 0), at(12, 0), null))).body(),
				SearchResponse.class);
		assertThat(search.options()).isNotEmpty();

		String confirmBody = json.writeValueAsString(new ConfirmRequest(service.serviceTypeId(), at(10, 0),
				new SubjectDto("PERSON", "Alex", null, null, null),
				List.of(new AssignmentDto(staffReq, staff), new AssignmentDto(roomReq, room))));
		assertThat(post("/v1/bookings", "admin", confirmBody).statusCode()).isEqualTo(201);
	}

	// --- helpers ---

	private static UUID requirementOf(CreatedServiceResponse service, String baseKind) {
		return service.requirements().stream().filter(r -> r.baseKind().equals(baseKind)).findFirst().orElseThrow()
				.requirementId();
	}

	private static UUID created(HttpResponse<String> response) throws Exception {
		assertThat(response.statusCode()).isEqualTo(201);
		return json.readValue(response.body(), IdResponse.class).id();
	}

	private static Instant at(int hour, int minute) {
		return DATE.atTime(hour, minute).toInstant(ZoneOffset.UTC);
	}

	private static HttpResponse<String> post(String path, String bearer, String body) throws Exception {
		HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
				.header("Authorization", "Bearer " + bearer).header("X-Tenant-Id", TENANT.toString())
				.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
		return HTTP.send(req, HttpResponse.BodyHandlers.ofString());
	}
}
