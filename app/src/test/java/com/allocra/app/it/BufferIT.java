package com.allocra.app.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.allocra.app.AllocraApplication;
import com.allocra.app.web.ApiModel.AssignmentDto;
import com.allocra.app.web.ApiModel.ConfirmRequest;
import com.allocra.app.web.ApiModel.SubjectDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
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
 * Verifies resource setup/cleanup buffers end-to-end against real PostgreSQL
 * (PRD-RES-012, RES-AT-005): a room with a 30-minute cleanup is reserved for
 * the buffered window, and a back-to-back booking that would fall inside that
 * buffer is rejected. (Lead and setup logic is additionally covered by the pure
 * DirectAvailabilitySearchTest.)
 */
class BufferIT {

	@SuppressWarnings("resource")
	private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

	private static ConfigurableApplicationContext ctx;
	private static ObjectMapper json;
	private static JdbcClient jdbc;
	private static int port;
	private static final HttpClient HTTP = HttpClient.newHttpClient();

	private static final UUID TENANT = UUID.randomUUID();
	private static final UUID SERVICE = UUID.randomUUID();
	private static final UUID STAFF_REQ = UUID.randomUUID();
	private static final UUID ROOM_REQ = UUID.randomUUID();
	private static final UUID STAFF = UUID.randomUUID();
	private static final UUID ROOM = UUID.randomUUID();

	@BeforeAll
	static void boot() {
		POSTGRES.start();
		ctx = new SpringApplicationBuilder(AllocraApplication.class).run("--server.port=0",
				"--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
				"--spring.datasource.username=" + POSTGRES.getUsername(),
				"--spring.datasource.password=" + POSTGRES.getPassword());
		port = ((ServletWebServerApplicationContext) ctx).getWebServer().getPort();
		json = ctx.getBean(ObjectMapper.class);
		jdbc = ctx.getBean(JdbcClient.class);
		seed();
	}

	@AfterAll
	static void shutdown() {
		if (ctx != null) {
			ctx.close();
		}
		POSTGRES.stop();
	}

	private static void seed() {
		ins("INSERT INTO tenant(id,slug,display_name) VALUES(?,?,?)", TENANT, "acme", "Acme");
		UUID userId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		ins("INSERT INTO application_user(id,firebase_uid,display_name) VALUES(?,?,?)", userId, "admin", "Admin");
		ins("INSERT INTO organisation_member(tenant_id,id,user_id,status) VALUES(?,?,?,?)", TENANT, memberId, userId,
				"ACTIVE");
		ins("INSERT INTO organisation_member_role(tenant_id,member_id,role) VALUES(?,?,?)", TENANT, memberId,
				"ORG_ADMIN");

		UUID staffType = UUID.randomUUID();
		UUID roomType = UUID.randomUUID();
		ins("INSERT INTO resource_type(tenant_id,id,code,base_kind) VALUES(?,?,?,?)", TENANT, staffType, "STAFF",
				"PERSON");
		ins("INSERT INTO resource_type(tenant_id,id,code,base_kind) VALUES(?,?,?,?)", TENANT, roomType, "ROOM",
				"PLACE");
		ins("INSERT INTO resource(tenant_id,id,resource_type_id,name) VALUES(?,?,?,?)", TENANT, STAFF, staffType,
				"Sam");
		// Room has a 30-minute cleanup/turnover buffer.
		ins("INSERT INTO resource(tenant_id,id,resource_type_id,name,cleanup_minutes) VALUES(?,?,?,?,?)", TENANT, ROOM,
				roomType, "Room A", 30);
		ins("INSERT INTO resource_capability(tenant_id,id,resource_id,capability_type) VALUES(?,?,?,?)", TENANT,
				UUID.randomUUID(), STAFF, "PHYSIO");

		for (UUID r : List.of(STAFF, ROOM)) {
			for (int day = 1; day <= 7; day++) {
				ins("INSERT INTO availability_rule(tenant_id,id,resource_id,day_of_week,start_time,end_time) VALUES(?,?,?,?,?,?)",
						TENANT, UUID.randomUUID(), r, day, LocalTime.of(0, 0), LocalTime.of(23, 59));
			}
		}

		ins("INSERT INTO service_type(tenant_id,id,code,name,duration_minutes) VALUES(?,?,?,?,?)", TENANT, SERVICE,
				"TREAT", "Treatment", 60);
		ins("INSERT INTO resource_requirement(tenant_id,id,service_type_id,base_kind,required,selection_mode,required_capability_type) VALUES(?,?,?,?,?,?,?)",
				TENANT, STAFF_REQ, SERVICE, "PERSON", Boolean.TRUE, "ANY", "PHYSIO");
		ins("INSERT INTO resource_requirement(tenant_id,id,service_type_id,base_kind,required,selection_mode) VALUES(?,?,?,?,?,?)",
				TENANT, ROOM_REQ, SERVICE, "PLACE", Boolean.TRUE, "ANY");
	}

	@Test
	@DisplayName("PRD-RES-012 / RES-AT-005: a cleanup buffer reserves an extended window and blocks a back-to-back booking")
	void cleanupBufferReservesExtendedWindowAndBlocksBackToBack() throws Exception {
		// Book 10:00–11:00; the room (30-min cleanup) is reserved to 11:30.
		assertThat(confirm(Instant.parse("2026-07-06T10:00:00Z")).statusCode()).isEqualTo(201);
		OffsetDateTime roomEnd = jdbc.sql(
				"SELECT end_at FROM reservation WHERE tenant_id=? AND resource_id=? AND status='ACTIVE' AND start_at=?")
				.params(TENANT, ROOM, OffsetDateTime.ofInstant(Instant.parse("2026-07-06T10:00:00Z"), ZoneOffset.UTC))
				.query(OffsetDateTime.class).single();
		assertThat(roomEnd.toInstant()).isEqualTo(Instant.parse("2026-07-06T11:30:00Z"));

		// 11:00–12:00 falls inside the room's cleanup buffer → reservation clash (409).
		assertThat(confirm(Instant.parse("2026-07-06T11:00:00Z")).statusCode()).isEqualTo(409);

		// 11:30–12:30 is clear of the buffer → bookable.
		assertThat(confirm(Instant.parse("2026-07-06T11:30:00Z")).statusCode()).isEqualTo(201);
	}

	private static HttpResponse<String> confirm(Instant start) throws Exception {
		String body = json.writeValueAsString(
				new ConfirmRequest(SERVICE, start, new SubjectDto("PERSON", "Alex", null, null, null),
						List.of(new AssignmentDto(STAFF_REQ, STAFF), new AssignmentDto(ROOM_REQ, ROOM))));
		HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v1/bookings"))
				.header("Authorization", "Bearer admin").header("X-Tenant-Id", TENANT.toString())
				.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
		return HTTP.send(req, HttpResponse.BodyHandlers.ofString());
	}

	private static void ins(String sql, Object... params) {
		jdbc.sql(sql).params(List.of(params)).update();
	}
}
