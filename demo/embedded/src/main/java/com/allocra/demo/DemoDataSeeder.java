package com.allocra.demo;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Seeds a sample organisation into the ephemeral demo database and prints a
 * cheat sheet so the API can be explored immediately (Swagger UI + bearer
 * tokens). Active only under the {@code demo} profile. Idempotency is
 * unnecessary — the demo database is fresh each run.
 */
@Component
@Profile("demo")
public class DemoDataSeeder implements ApplicationRunner {

	private final JdbcClient jdbc;
	private final Environment env;

	public DemoDataSeeder(JdbcClient jdbc, Environment env) {
		this.jdbc = jdbc;
		this.env = env;
	}

	@Override
	public void run(ApplicationArguments args) {
		UUID tenant = UUID.randomUUID();
		ins("INSERT INTO tenant(id,slug,display_name) VALUES(?,?,?)", tenant, "demo", "Demo Physio Clinic");

		member(tenant, "admin", "Alex Admin", "ORG_ADMIN");
		member(tenant, "scheduler", "Sam Scheduler", "SCHEDULER");
		member(tenant, "viewer", "Vic Viewer", "VIEWER");

		UUID location = id();
		ins("INSERT INTO location(tenant_id,id,name) VALUES(?,?,?)", tenant, location, "Main Site");

		UUID staffType = resourceType(tenant, "STAFF", "PERSON");
		UUID roomType = resourceType(tenant, "ROOM", "PLACE");
		UUID equipType = resourceType(tenant, "EQUIPMENT", "ASSET");

		UUID sam = resource(tenant, staffType, location, "Sam (Physio)");
		capability(tenant, sam, "PHYSIO");
		UUID dana = resource(tenant, staffType, location, "Dana (Physio + Sports)");
		capability(tenant, dana, "PHYSIO");
		capability(tenant, dana, "SPORTS");
		UUID roomA = resource(tenant, roomType, location, "Room A");
		capability(tenant, roomA, "PRIVATE");
		UUID roomB = resource(tenant, roomType, location, "Room B");
		UUID ultrasound = resource(tenant, equipType, location, "Ultrasound Unit");
		capability(tenant, ultrasound, "ULTRASOUND");

		// Available every day 08:00–20:00 so any near-future slot is bookable.
		for (UUID r : List.of(sam, dana, roomA, roomB, ultrasound)) {
			for (int day = 1; day <= 7; day++) {
				ins("INSERT INTO availability_rule(tenant_id,id,resource_id,day_of_week,start_time,end_time) VALUES(?,?,?,?,?,?)",
						tenant, id(), r, day, LocalTime.of(8, 0), LocalTime.of(20, 0));
			}
		}

		UUID service = id();
		ins("INSERT INTO service_type(tenant_id,id,code,name,duration_minutes) VALUES(?,?,?,?,?)", tenant, service,
				"PHYSIO", "Physio Session", 60);
		UUID staffReq = requirement(tenant, service, "PERSON", true, "PHYSIO");
		UUID roomReq = requirement(tenant, service, "PLACE", true, null);
		UUID equipReq = requirement(tenant, service, "ASSET", false, "ULTRASOUND");

		printCheatSheet(tenant, service, staffReq, roomReq, equipReq, sam, roomA, ultrasound);
	}

	private void member(UUID tenant, String firebaseUid, String displayName, String role) {
		UUID userId = id();
		UUID memberId = id();
		ins("INSERT INTO application_user(id,firebase_uid,display_name) VALUES(?,?,?)", userId, firebaseUid,
				displayName);
		ins("INSERT INTO organisation_member(tenant_id,id,user_id,status) VALUES(?,?,?,?)", tenant, memberId, userId,
				"ACTIVE");
		ins("INSERT INTO organisation_member_role(tenant_id,member_id,role) VALUES(?,?,?)", tenant, memberId, role);
	}

	private UUID resourceType(UUID tenant, String code, String baseKind) {
		UUID id = id();
		ins("INSERT INTO resource_type(tenant_id,id,code,base_kind) VALUES(?,?,?,?)", tenant, id, code, baseKind);
		return id;
	}

	private UUID resource(UUID tenant, UUID typeId, UUID locationId, String name) {
		UUID id = id();
		ins("INSERT INTO resource(tenant_id,id,resource_type_id,name,location_id) VALUES(?,?,?,?,?)", tenant, id,
				typeId, name, locationId);
		return id;
	}

	private void capability(UUID tenant, UUID resourceId, String type) {
		ins("INSERT INTO resource_capability(tenant_id,id,resource_id,capability_type) VALUES(?,?,?,?)", tenant, id(),
				resourceId, type);
	}

	private UUID requirement(UUID tenant, UUID service, String baseKind, boolean required, String capabilityType) {
		UUID id = id();
		if (capabilityType == null) {
			ins("INSERT INTO resource_requirement(tenant_id,id,service_type_id,base_kind,required,selection_mode) VALUES(?,?,?,?,?,?)",
					tenant, id, service, baseKind, required, "ANY");
		} else {
			ins("INSERT INTO resource_requirement(tenant_id,id,service_type_id,base_kind,required,selection_mode,required_capability_type) VALUES(?,?,?,?,?,?,?)",
					tenant, id, service, baseKind, required, "ANY", capabilityType);
		}
		return id;
	}

	private void ins(String sql, Object... params) {
		jdbc.sql(sql).params(List.of(params)).update();
	}

	private static UUID id() {
		return UUID.randomUUID();
	}

	private void printCheatSheet(UUID tenant, UUID service, UUID staffReq, UUID roomReq, UUID equipReq, UUID sam,
			UUID roomA, UUID ultrasound) {
		String base = "http://localhost:" + env.getProperty("local.server.port", "8080");
		Instant slot = LocalDate.now(ZoneOffset.UTC).plusDays(1).atTime(10, 0).toInstant(ZoneOffset.UTC);
		Instant from = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
		Instant to = LocalDate.now(ZoneOffset.UTC).plusDays(7).atStartOfDay().toInstant(ZoneOffset.UTC);

		String header = "X-Tenant-Id: " + tenant;
		String bearer = "Authorization: Bearer scheduler";

		StringBuilder b = new StringBuilder();
		b.append("\n");
		b.append("======================================================================\n");
		b.append("  ALLOCRA DEMO — ready to explore (embedded PostgreSQL, no Docker)\n");
		b.append("======================================================================\n");
		b.append("  Swagger UI : ").append(base).append("/swagger-ui\n");
		b.append("  OpenAPI    : ").append(base).append("/v3/api-docs\n");
		b.append("  Health     : ").append(base).append("/actuator/health\n");
		b.append("----------------------------------------------------------------------\n");
		b.append("  Tenant id  : ").append(tenant).append("   (send as header 'X-Tenant-Id')\n");
		b.append("  Tokens     : send 'Authorization: Bearer <token>' — the token IS the user\n");
		b.append("               admin      (all permissions)\n");
		b.append("               scheduler  (create/cancel/reschedule/complete bookings)\n");
		b.append("               viewer     (read only)\n");
		b.append("----------------------------------------------------------------------\n");
		b.append("  Service    : Physio Session  id=").append(service).append("\n");
		b.append("    staff requirement id : ").append(staffReq).append("\n");
		b.append("    room  requirement id : ").append(roomReq).append("\n");
		b.append("    equip requirement id : ").append(equipReq).append("  (optional)\n");
		b.append("  Sample resources: Sam=").append(sam).append("  RoomA=").append(roomA).append("  Ultrasound=")
				.append(ultrasound).append("\n");
		b.append("----------------------------------------------------------------------\n");
		b.append("  1) Search availability:\n");
		b.append("     curl -s -X POST '").append(base).append("/v1/services/").append(service)
				.append("/availability/search' \\\n");
		b.append("       -H '").append(header).append("' -H '").append(bearer).append("' \\\n");
		b.append("       -H 'Content-Type: application/json' \\\n");
		b.append("       -d '{\"from\":\"").append(from).append("\",\"to\":\"").append(to).append("\"}'\n");
		b.append("\n");
		b.append("  2) Confirm a booking (staff=Sam, room=RoomA at ").append(slot).append("):\n");
		b.append("     curl -s -X POST '").append(base).append("/v1/bookings' \\\n");
		b.append("       -H '").append(header).append("' -H '").append(bearer).append("' \\\n");
		b.append("       -H 'Content-Type: application/json' \\\n");
		b.append("       -d '{\"serviceTypeId\":\"").append(service).append("\",\"start\":\"").append(slot)
				.append("\",\"subject\":{\"type\":\"PERSON\",\"displayName\":\"Alex\"},")
				.append("\"assignments\":[{\"requirementId\":\"").append(staffReq).append("\",\"resourceId\":\"")
				.append(sam).append("\"},{\"requirementId\":\"").append(roomReq).append("\",\"resourceId\":\"")
				.append(roomA).append("\"}]}'\n");
		b.append("\n");
		b.append("  3) List bookings:  curl -s '").append(base).append("/v1/bookings' -H '").append(header)
				.append("' -H '").append(bearer).append("'\n");
		b.append("======================================================================\n");
		System.out.println(b);
	}
}
