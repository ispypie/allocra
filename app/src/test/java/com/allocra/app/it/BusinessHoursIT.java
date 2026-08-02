package com.allocra.app.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.allocra.app.AllocraApplication;
import com.allocra.app.web.ApiModel.AssignmentDto;
import com.allocra.app.web.ApiModel.ConfirmRequest;
import com.allocra.app.web.ApiModel.SubjectDto;
import com.allocra.app.web.ConfigApi.CreateClosureRequest;
import com.allocra.app.web.ConfigApi.CreateLocationRequest;
import com.allocra.app.web.ConfigApi.CreateOperatingHoursRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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
 * Location business hours, closures and timezone (PRD-AVL-005/006/007,
 * AVL-AT-003/004). The location is Europe/London and open 09:00–17:00 local; in
 * July (BST = UTC+1) that is 08:00–16:00 UTC, which is what these assertions
 * rely on to prove timezone handling.
 */
class BusinessHoursIT {

	@SuppressWarnings("resource")
	private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

	private static ConfigurableApplicationContext ctx;
	private static ObjectMapper json;
	private static int port;
	private static final HttpClient HTTP = HttpClient.newHttpClient();

	private static final UUID TENANT = UUID.randomUUID();
	private static final UUID SERVICE = UUID.randomUUID();
	private static final UUID STAFF_REQ = UUID.randomUUID();
	private static final UUID ROOM_REQ = UUID.randomUUID();
	private static final UUID STAFF = UUID.randomUUID();
	private static final UUID ROOM = UUID.randomUUID();
	private static final UUID LOCATION = UUID.randomUUID();

	@BeforeAll
	static void boot() {
		POSTGRES.start();
		ctx = new SpringApplicationBuilder(AllocraApplication.class).run("--server.port=0",
				"--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
				"--spring.datasource.username=" + POSTGRES.getUsername(),
				"--spring.datasource.password=" + POSTGRES.getPassword());
		port = ((ServletWebServerApplicationContext) ctx).getWebServer().getPort();
		json = ctx.getBean(ObjectMapper.class);
		seed(ctx.getBean(JdbcClient.class));
	}

	@AfterAll
	static void shutdown() {
		if (ctx != null) {
			ctx.close();
		}
		POSTGRES.stop();
	}

	private static void seed(JdbcClient jdbc) {
		ins(jdbc, "INSERT INTO tenant(id,slug,display_name) VALUES(?,?,?)", TENANT, "acme", "Acme");
		member(jdbc, "admin", "ORG_ADMIN");
		member(jdbc, "viewer", "VIEWER");

		ins(jdbc, "INSERT INTO location(tenant_id,id,name,timezone) VALUES(?,?,?,?)", TENANT, LOCATION, "London Clinic",
				"Europe/London");

		UUID staffType = UUID.randomUUID();
		UUID roomType = UUID.randomUUID();
		ins(jdbc, "INSERT INTO resource_type(tenant_id,id,code,base_kind) VALUES(?,?,?,?)", TENANT, staffType, "STAFF",
				"PERSON");
		ins(jdbc, "INSERT INTO resource_type(tenant_id,id,code,base_kind) VALUES(?,?,?,?)", TENANT, roomType, "ROOM",
				"PLACE");
		ins(jdbc, "INSERT INTO resource(tenant_id,id,resource_type_id,name,location_id) VALUES(?,?,?,?,?)", TENANT,
				STAFF, staffType, "Sam", LOCATION);
		ins(jdbc, "INSERT INTO resource(tenant_id,id,resource_type_id,name,location_id) VALUES(?,?,?,?,?)", TENANT,
				ROOM, roomType, "Room A", LOCATION);
		ins(jdbc, "INSERT INTO resource_capability(tenant_id,id,resource_id,capability_type) VALUES(?,?,?,?)", TENANT,
				UUID.randomUUID(), STAFF, "PHYSIO");

		// Resources available all day; the binding constraint is the location's
		// operating hours.
		for (UUID r : List.of(STAFF, ROOM)) {
			for (int day = 1; day <= 7; day++) {
				ins(jdbc,
						"INSERT INTO availability_rule(tenant_id,id,resource_id,day_of_week,start_time,end_time) VALUES(?,?,?,?,?,?)",
						TENANT, UUID.randomUUID(), r, day, LocalTime.of(0, 0), LocalTime.of(23, 59));
			}
		}
		// Location open 09:00–17:00 local (Europe/London) every day.
		for (int day = 1; day <= 7; day++) {
			ins(jdbc,
					"INSERT INTO location_operating_hours(tenant_id,id,location_id,day_of_week,open_time,close_time) VALUES(?,?,?,?,?,?)",
					TENANT, UUID.randomUUID(), LOCATION, day, LocalTime.of(9, 0), LocalTime.of(17, 0));
		}

		ins(jdbc, "INSERT INTO location_closure(tenant_id,id,location_id,start_date,end_date) VALUES(?,?,?,?,?)",
				TENANT, UUID.randomUUID(), LOCATION, LocalDate.of(2026, 7, 7), LocalDate.of(2026, 7, 7));

		ins(jdbc, "INSERT INTO service_type(tenant_id,id,code,name,duration_minutes) VALUES(?,?,?,?,?)", TENANT,
				SERVICE, "TREAT", "Treatment", 60);
		ins(jdbc,
				"INSERT INTO resource_requirement(tenant_id,id,service_type_id,base_kind,required,selection_mode,required_capability_type) VALUES(?,?,?,?,?,?,?)",
				TENANT, STAFF_REQ, SERVICE, "PERSON", Boolean.TRUE, "ANY", "PHYSIO");
		ins(jdbc,
				"INSERT INTO resource_requirement(tenant_id,id,service_type_id,base_kind,required,selection_mode) VALUES(?,?,?,?,?,?)",
				TENANT, ROOM_REQ, SERVICE, "PLACE", Boolean.TRUE, "ANY");
	}

	@Test
	@DisplayName("PRD-AVL-005/007 / AVL-AT-003: a slot within the location's operating hours (timezone-aware) is bookable")
	void withinOperatingHoursBookable() throws Exception {
		// 08:00Z on a July day = 09:00 BST = opening time.
		assertThat(confirm(Instant.parse("2026-07-06T08:00:00Z")).statusCode()).isEqualTo(201);
	}

	@Test
	@DisplayName("PRD-AVL-005/007 / AVL-AT-003: a slot before opening (in the location timezone) is rejected")
	void beforeOpeningRejected() throws Exception {
		// 07:00Z = 08:00 BST, before the 09:00 local opening.
		assertThat(confirm(Instant.parse("2026-07-06T07:00:00Z")).statusCode()).isEqualTo(422);
	}

	@Test
	@DisplayName("PRD-AVL-005/007: a slot running past closing (in the location timezone) is rejected")
	void pastClosingRejected() throws Exception {
		// 15:30Z–16:30Z = 16:30–17:30 BST; runs past the 17:00 local close.
		assertThat(confirm(Instant.parse("2026-07-06T15:30:00Z")).statusCode()).isEqualTo(422);
	}

	@Test
	@DisplayName("PRD-AVL-006 / AVL-AT-004: a slot on a location closure day is rejected")
	void closureDayRejected() throws Exception {
		// 08:00Z on 2026-07-07 is within hours, but the location is closed that day.
		assertThat(confirm(Instant.parse("2026-07-07T08:00:00Z")).statusCode()).isEqualTo(422);
	}

	@Test
	@DisplayName("PRD-AVL-005/006: operating-hours and closure endpoints are permission-gated (admin 201, viewer 403)")
	void configEndpointsArePermissionGated() throws Exception {
		HttpResponse<String> created = post("/v1/locations", "admin",
				json.writeValueAsString(new CreateLocationRequest("Second Site", "Europe/London")));
		assertThat(created.statusCode()).isEqualTo(201);
		UUID location2 = UUID.fromString(json.readTree(created.body()).get("id").asText());

		assertThat(post("/v1/locations/" + location2 + "/operating-hours", "admin",
				json.writeValueAsString(new CreateOperatingHoursRequest(1, LocalTime.of(8, 0), LocalTime.of(9, 0))))
				.statusCode()).isEqualTo(201);
		assertThat(post("/v1/locations/" + location2 + "/closures", "admin",
				json.writeValueAsString(
						new CreateClosureRequest(LocalDate.of(2026, 12, 25), LocalDate.of(2026, 12, 25), "Christmas")))
				.statusCode()).isEqualTo(201);
		assertThat(post("/v1/locations/" + location2 + "/operating-hours", "viewer",
				json.writeValueAsString(new CreateOperatingHoursRequest(1, LocalTime.of(8, 0), LocalTime.of(9, 0))))
				.statusCode()).isEqualTo(403);
	}

	// --- helpers ---

	private static HttpResponse<String> confirm(Instant start) throws Exception {
		String body = json.writeValueAsString(
				new ConfirmRequest(SERVICE, start, new SubjectDto("PERSON", "Alex", null, null, null),
						List.of(new AssignmentDto(STAFF_REQ, STAFF), new AssignmentDto(ROOM_REQ, ROOM))));
		return post("/v1/bookings", "admin", body);
	}

	private static HttpResponse<String> post(String path, String bearer, String body) throws Exception {
		HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
				.header("Authorization", "Bearer " + bearer).header("X-Tenant-Id", TENANT.toString())
				.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
		return HTTP.send(req, HttpResponse.BodyHandlers.ofString());
	}

	private static void member(JdbcClient jdbc, String firebaseUid, String role) {
		UUID userId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		ins(jdbc, "INSERT INTO application_user(id,firebase_uid,display_name) VALUES(?,?,?)", userId, firebaseUid,
				firebaseUid);
		ins(jdbc, "INSERT INTO organisation_member(tenant_id,id,user_id,status) VALUES(?,?,?,?)", TENANT, memberId,
				userId, "ACTIVE");
		ins(jdbc, "INSERT INTO organisation_member_role(tenant_id,member_id,role) VALUES(?,?,?)", TENANT, memberId,
				role);
	}

	private static void ins(JdbcClient jdbc, String sql, Object... params) {
		jdbc.sql(sql).params(List.of(params)).update();
	}
}
