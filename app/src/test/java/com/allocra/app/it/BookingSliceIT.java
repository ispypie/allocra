package com.allocra.app.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.allocra.app.AllocraApplication;
import com.allocra.app.application.ConfirmBookingService;
import com.allocra.app.application.ConfirmBookingService.ConfirmCommand;
import com.allocra.app.web.ApiModel.AssignmentDto;
import com.allocra.app.web.ApiModel.BookingDto;
import com.allocra.app.web.ApiModel.BookingListResponse;
import com.allocra.app.web.ApiModel.ConfirmRequest;
import com.allocra.app.web.ApiModel.RescheduleRequest;
import com.allocra.app.web.ApiModel.SearchRequest;
import com.allocra.app.web.ApiModel.SearchResponse;
import com.allocra.app.web.ApiModel.SubjectDto;
import com.allocra.bookings.BookingSubject;
import com.allocra.common.tenant.TenantId;
import com.allocra.membership.Membership;
import com.allocra.membership.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
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
 * End-to-end slice test against real PostgreSQL: the internal booking flow
 * (authenticated search + confirm), tenant isolation, permission checks and
 * concurrency. The app is booted programmatically (see
 * {@link ApplicationSmokeIT} for why) and driven over HTTP; the true
 * concurrency race is exercised via the transactional service bean.
 */
class BookingSliceIT {

	@SuppressWarnings("resource")
	private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

	private static ConfigurableApplicationContext ctx;
	private static JdbcClient jdbc;
	private static ObjectMapper json;
	private static int port;
	private static final HttpClient HTTP = HttpClient.newHttpClient();

	private static final UUID TENANT_A = UUID.randomUUID();
	private static final UUID TENANT_B = UUID.randomUUID();
	private static final UUID USER_SCHED = UUID.randomUUID();
	private static final UUID MEMBER_SCHED = UUID.randomUUID();
	private static final UUID S1 = UUID.randomUUID();
	private static final UUID R1 = UUID.randomUUID();
	private static final UUID SERVICE = UUID.randomUUID();
	private static final UUID STAFF_REQ = UUID.randomUUID();
	private static final UUID ROOM_REQ = UUID.randomUUID();

	private static final LocalDate DATE = LocalDate.of(2026, 9, 7);

	@BeforeAll
	static void boot() {
		POSTGRES.start();
		ctx = new SpringApplicationBuilder(AllocraApplication.class).run("--server.port=0",
				"--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
				"--spring.datasource.username=" + POSTGRES.getUsername(),
				"--spring.datasource.password=" + POSTGRES.getPassword());
		port = ((ServletWebServerApplicationContext) ctx).getWebServer().getPort();
		jdbc = ctx.getBean(JdbcClient.class);
		json = ctx.getBean(ObjectMapper.class);
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
		int dow = DATE.getDayOfWeek().getValue();
		ins("INSERT INTO tenant(id,slug,display_name) VALUES(?,?,?)", TENANT_A, "acme-a", "Acme A");
		ins("INSERT INTO tenant(id,slug,display_name) VALUES(?,?,?)", TENANT_B, "acme-b", "Acme B");
		ins("INSERT INTO application_user(id,firebase_uid,display_name) VALUES(?,?,?)", USER_SCHED, "sched", "Sam");
		UUID userViewer = UUID.randomUUID();
		ins("INSERT INTO application_user(id,firebase_uid,display_name) VALUES(?,?,?)", userViewer, "viewer", "Vic");
		ins("INSERT INTO organisation_member(tenant_id,id,user_id,status) VALUES(?,?,?,?)", TENANT_A, MEMBER_SCHED,
				USER_SCHED, "ACTIVE");
		ins("INSERT INTO organisation_member_role(tenant_id,member_id,role) VALUES(?,?,?)", TENANT_A, MEMBER_SCHED,
				"SCHEDULER");
		UUID memberViewer = UUID.randomUUID();
		ins("INSERT INTO organisation_member(tenant_id,id,user_id,status) VALUES(?,?,?,?)", TENANT_A, memberViewer,
				userViewer, "ACTIVE");
		ins("INSERT INTO organisation_member_role(tenant_id,member_id,role) VALUES(?,?,?)", TENANT_A, memberViewer,
				"VIEWER");

		UUID staffType = UUID.randomUUID();
		UUID roomType = UUID.randomUUID();
		ins("INSERT INTO resource_type(tenant_id,id,code,base_kind) VALUES(?,?,?,?)", TENANT_A, staffType, "STAFF",
				"PERSON");
		ins("INSERT INTO resource_type(tenant_id,id,code,base_kind) VALUES(?,?,?,?)", TENANT_A, roomType, "ROOM",
				"PLACE");
		ins("INSERT INTO resource(tenant_id,id,resource_type_id,name) VALUES(?,?,?,?)", TENANT_A, S1, staffType,
				"Sam Physio");
		ins("INSERT INTO resource(tenant_id,id,resource_type_id,name) VALUES(?,?,?,?)", TENANT_A, R1, roomType,
				"Room 1");
		ins("INSERT INTO resource_capability(tenant_id,id,resource_id,capability_type) VALUES(?,?,?,?)", TENANT_A,
				UUID.randomUUID(), S1, "PHYSIO");
		for (UUID r : List.of(S1, R1)) {
			ins("INSERT INTO availability_rule(tenant_id,id,resource_id,day_of_week,start_time,end_time) VALUES(?,?,?,?,?,?)",
					TENANT_A, UUID.randomUUID(), r, dow, LocalTime.of(8, 0), LocalTime.of(18, 0));
		}
		ins("INSERT INTO service_type(tenant_id,id,code,name,duration_minutes) VALUES(?,?,?,?,?)", TENANT_A, SERVICE,
				"TREAT", "Treatment", 60);
		ins("INSERT INTO resource_requirement(tenant_id,id,service_type_id,base_kind,required,selection_mode,required_capability_type) VALUES(?,?,?,?,?,?,?)",
				TENANT_A, STAFF_REQ, SERVICE, "PERSON", Boolean.TRUE, "ANY", "PHYSIO");
		ins("INSERT INTO resource_requirement(tenant_id,id,service_type_id,base_kind,required,selection_mode) VALUES(?,?,?,?,?,?)",
				TENANT_A, ROOM_REQ, SERVICE, "PLACE", Boolean.TRUE, "ANY");
	}

	@Test
	@DisplayName("PRD-BKG-004 / BKG-AT-004: authenticated search then confirm creates a booking, assignments and reservations; the slot is then taken")
	void searchThenConfirmThenSlotTaken() throws Exception {
		Instant start = at(10, 0);
		SearchResponse search = json.readValue(
				post("/v1/services/" + SERVICE + "/availability/search", "sched", TENANT_A,
						json.writeValueAsString(new SearchRequest(at(9, 0), at(12, 0), null))).body(),
				SearchResponse.class);
		assertThat(search.options()).isNotEmpty();

		HttpResponse<String> confirm = post("/v1/bookings", "sched", TENANT_A, confirmBody(start));
		assertThat(confirm.statusCode()).isEqualTo(201);
		UUID bookingId = UUID.fromString(json.readTree(confirm.body()).get("bookingId").asText());

		assertThat(count("resource_assignment", bookingId)).isEqualTo(2);
		assertThat(count("reservation", bookingId)).isEqualTo(2);

		// Re-confirming the same slot now clashes with the reservation → 409 Conflict.
		assertThat(post("/v1/bookings", "sched", TENANT_A, confirmBody(start)).statusCode()).isEqualTo(409);
	}

	@Test
	@DisplayName("PRD-RSV-004 / RSV-AT-002: concurrent confirmations for the same resource — only one succeeds")
	void concurrentConfirmsOnlyOneSucceeds() throws Exception {
		Instant start = at(14, 0);
		ConfirmBookingService service = ctx.getBean(ConfirmBookingService.class);
		Membership scheduler = new Membership(TenantId.of(TENANT_A), MEMBER_SCHED, USER_SCHED, Set.of(Role.SCHEDULER));
		ConfirmCommand command = new ConfirmCommand(SERVICE, start,
				new BookingSubject("PERSON", "Alex", null, null, null), Map.of(STAFF_REQ, S1, ROOM_REQ, R1));

		ExecutorService pool = Executors.newFixedThreadPool(2);
		CountDownLatch go = new CountDownLatch(1);
		Callable<Object> task = () -> {
			go.await();
			try {
				return service.confirm(TenantId.of(TENANT_A), scheduler, command);
			} catch (RuntimeException e) {
				return e;
			}
		};
		Future<Object> f1 = pool.submit(task);
		Future<Object> f2 = pool.submit(task);
		go.countDown();
		Object r1 = f1.get();
		Object r2 = f2.get();
		pool.shutdown();

		long successes = Stream.of(r1, r2).filter(o -> o instanceof UUID).count();
		assertThat(successes).isEqualTo(1);
		// The loser failed with a reservation conflict (the DB exclusion constraint →
		// 409).
		assertThat(Stream.of(r1, r2).anyMatch(o -> o instanceof RuntimeException)).isTrue();
		// Exactly one ACTIVE reservation exists for that resource/slot — no double
		// booking.
		Integer active = jdbc.sql(
				"SELECT count(*) FROM reservation WHERE tenant_id=? AND resource_id=? AND status='ACTIVE' AND start_at=?")
				.params(List.of(TENANT_A, S1, OffsetDateTime.ofInstant(start, ZoneOffset.UTC))).query(Integer.class)
				.single();
		assertThat(active).isEqualTo(1);
	}

	@Test
	@DisplayName("PRD-TEN-004 / TEN-AT-002: a user acting on a tenant they do not belong to is rejected")
	void crossTenantRejected() throws Exception {
		// 'sched' is a member of tenant A only; using tenant B's id must be refused.
		assertThat(post("/v1/bookings", "sched", TENANT_B, confirmBody(at(16, 0))).statusCode()).isEqualTo(403);
	}

	@Test
	@DisplayName("PRD-MEM-004 / MEM-AT-001: a viewer without BOOKING_CREATE cannot confirm")
	void viewerCannotConfirm() throws Exception {
		assertThat(post("/v1/bookings", "viewer", TENANT_A, confirmBody(at(17, 0))).statusCode()).isEqualTo(403);
	}

	@Test
	@DisplayName("PRD-SEC-001: an unauthenticated request is rejected")
	void unauthenticatedRejected() throws Exception {
		HttpRequest req = HttpRequest.newBuilder(URI.create(base() + "/v1/bookings"))
				.header("X-Tenant-Id", TENANT_A.toString()).header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(confirmBody(at(11, 0)))).build();
		assertThat(HTTP.send(req, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
	}

	@Test
	@DisplayName("PRD-BKG-008 / BKG-AT-007: cancelling releases reservations and frees the slot for rebooking")
	void cancelReleasesReservationsAndFreesSlot() throws Exception {
		Instant start = at(15, 0);
		UUID bookingId = confirmBooking(start);
		assertThat(activeReservations(bookingId)).isEqualTo(2);

		assertThat(post("/v1/bookings/" + bookingId + "/cancel", "sched", TENANT_A, "").statusCode()).isEqualTo(200);
		assertThat(bookingStatus(bookingId)).isEqualTo("CANCELLED");
		assertThat(activeReservations(bookingId)).isEqualTo(0);

		// The freed slot can be booked again.
		assertThat(post("/v1/bookings", "sched", TENANT_A, confirmBody(start)).statusCode()).isEqualTo(201);
	}

	@Test
	@DisplayName("PRD-BKG-009 / BKG-AT-008: complete and no-show transitions; invalid transitions are rejected")
	void completeAndNoShowAndInvalidTransition() throws Exception {
		UUID toComplete = confirmBooking(at(13, 0));
		assertThat(post("/v1/bookings/" + toComplete + "/complete", "sched", TENANT_A, "").statusCode()).isEqualTo(200);
		assertThat(bookingStatus(toComplete)).isEqualTo("COMPLETED");

		UUID toNoShow = confirmBooking(at(12, 0));
		assertThat(post("/v1/bookings/" + toNoShow + "/no-show", "sched", TENANT_A, "").statusCode()).isEqualTo(200);
		assertThat(bookingStatus(toNoShow)).isEqualTo("NO_SHOW");

		// A completed booking cannot be cancelled (invalid transition → 409).
		assertThat(post("/v1/bookings/" + toComplete + "/cancel", "sched", TENANT_A, "").statusCode()).isEqualTo(409);
	}

	@Test
	@DisplayName("PRD-BKG-008 / PRD-MEM-004: a viewer cannot cancel a booking")
	void viewerCannotCancel() throws Exception {
		// 11:00 slot is isolated from the slots other tests book (avoids
		// order-dependent overlap).
		UUID bookingId = confirmBooking(at(11, 0));
		assertThat(post("/v1/bookings/" + bookingId + "/cancel", "viewer", TENANT_A, "").statusCode()).isEqualTo(403);
	}

	@Test
	@DisplayName("PRD-BKG-008: cancelling an unknown booking returns 404")
	void cancelUnknownBookingIsNotFound() throws Exception {
		assertThat(post("/v1/bookings/" + UUID.randomUUID() + "/cancel", "sched", TENANT_A, "").statusCode())
				.isEqualTo(404);
	}

	@Test
	@DisplayName("PRD-BKG-010 / BKG-AT-009: a booking can be retrieved by id with its assignments; unknown id is 404")
	void getBookingReturnsAssignments() throws Exception {
		UUID bookingId = confirmBooking(at(16, 0));

		HttpResponse<String> response = get("/v1/bookings/" + bookingId, "sched", TENANT_A);
		assertThat(response.statusCode()).isEqualTo(200);
		BookingDto dto = json.readValue(response.body(), BookingDto.class);
		assertThat(dto.status()).isEqualTo("CONFIRMED");
		assertThat(dto.assignments()).hasSize(2);

		// A viewer (BOOKING_VIEW) may read; an unknown id is 404.
		assertThat(get("/v1/bookings/" + bookingId, "viewer", TENANT_A).statusCode()).isEqualTo(200);
		assertThat(get("/v1/bookings/" + UUID.randomUUID(), "sched", TENANT_A).statusCode()).isEqualTo(404);
	}

	@Test
	@DisplayName("PRD-BKG-011 / BKG-AT-010: bookings can be listed for the active tenant, optionally filtered by status")
	void listBookings() throws Exception {
		UUID bookingId = confirmBooking(at(17, 0));

		BookingListResponse all = json.readValue(get("/v1/bookings", "sched", TENANT_A).body(),
				BookingListResponse.class);
		assertThat(all.bookings()).anyMatch(b -> b.id().equals(bookingId));

		BookingListResponse confirmed = json.readValue(get("/v1/bookings?status=CONFIRMED", "sched", TENANT_A).body(),
				BookingListResponse.class);
		assertThat(confirmed.bookings()).anyMatch(b -> b.id().equals(bookingId));
		assertThat(confirmed.bookings()).allMatch(b -> b.status().equals("CONFIRMED"));
	}

	@Test
	@DisplayName("PRD-BKG-012 / BKG-AT-011: rescheduling moves the booking (identity kept), frees the old slot, and rejects a taken new slot")
	void rescheduleMovesBookingAndFreesOldSlot() throws Exception {
		// Isolated early slots (08:00, 09:00) not used by other tests.
		UUID bookingId = confirmBooking(at(8, 0));

		// A viewer cannot reschedule.
		assertThat(post("/v1/bookings/" + bookingId + "/reschedule", "viewer", TENANT_A, rescheduleBody(at(9, 0)))
				.statusCode()).isEqualTo(403);

		// Reschedule 08:00 → 09:00 keeps the same booking id.
		assertThat(post("/v1/bookings/" + bookingId + "/reschedule", "sched", TENANT_A, rescheduleBody(at(9, 0)))
				.statusCode()).isEqualTo(200);
		BookingDto moved = json.readValue(get("/v1/bookings/" + bookingId, "sched", TENANT_A).body(), BookingDto.class);
		assertThat(moved.start()).isEqualTo(at(9, 0));

		// The old 08:00 slot is free again and can be booked by another booking.
		UUID blocker = confirmBooking(at(8, 0));
		assertThat(blocker).isNotEqualTo(bookingId);

		// Rescheduling onto the now-taken 08:00 slot clashes (→ 409); the booking is
		// left unchanged.
		assertThat(post("/v1/bookings/" + bookingId + "/reschedule", "sched", TENANT_A, rescheduleBody(at(8, 0)))
				.statusCode()).isEqualTo(409);
		BookingDto unchanged = json.readValue(get("/v1/bookings/" + bookingId, "sched", TENANT_A).body(),
				BookingDto.class);
		assertThat(unchanged.start()).isEqualTo(at(9, 0));
	}

	// --- helpers ---

	private static String rescheduleBody(Instant start) throws Exception {
		return json.writeValueAsString(new RescheduleRequest(start));
	}

	private static HttpResponse<String> get(String path, String bearer, UUID tenant) throws Exception {
		HttpRequest req = HttpRequest.newBuilder(URI.create(base() + path)).header("Authorization", "Bearer " + bearer)
				.header("X-Tenant-Id", tenant.toString()).GET().build();
		return HTTP.send(req, HttpResponse.BodyHandlers.ofString());
	}

	private static UUID confirmBooking(Instant start) throws Exception {
		HttpResponse<String> response = post("/v1/bookings", "sched", TENANT_A, confirmBody(start));
		assertThat(response.statusCode()).isEqualTo(201);
		return UUID.fromString(json.readTree(response.body()).get("bookingId").asText());
	}

	private static String bookingStatus(UUID bookingId) {
		return jdbc.sql("SELECT status FROM booking WHERE tenant_id=? AND id=?").params(List.of(TENANT_A, bookingId))
				.query(String.class).single();
	}

	private static Integer activeReservations(UUID bookingId) {
		return jdbc.sql("SELECT count(*) FROM reservation WHERE tenant_id=? AND booking_id=? AND status='ACTIVE'")
				.params(List.of(TENANT_A, bookingId)).query(Integer.class).single();
	}

	private static String confirmBody(Instant start) throws Exception {
		return json.writeValueAsString(
				new ConfirmRequest(SERVICE, start, new SubjectDto("PERSON", "Alex", null, null, null),
						List.of(new AssignmentDto(STAFF_REQ, S1), new AssignmentDto(ROOM_REQ, R1))));
	}

	private static Instant at(int hour, int minute) {
		return DATE.atTime(hour, minute).toInstant(ZoneOffset.UTC);
	}

	private static HttpResponse<String> post(String path, String bearer, UUID tenant, String body) throws Exception {
		HttpRequest req = HttpRequest.newBuilder(URI.create(base() + path)).header("Authorization", "Bearer " + bearer)
				.header("X-Tenant-Id", tenant.toString()).header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body)).build();
		return HTTP.send(req, HttpResponse.BodyHandlers.ofString());
	}

	private static String base() {
		return "http://localhost:" + port;
	}

	private static Integer count(String table, UUID bookingId) {
		return jdbc.sql("SELECT count(*) FROM " + table + " WHERE tenant_id=? AND booking_id=?")
				.params(List.of(TENANT_A, bookingId)).query(Integer.class).single();
	}

	private static void ins(String sql, Object... params) {
		jdbc.sql(sql).params(List.of(params)).update();
	}
}
