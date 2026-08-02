package com.allocra.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pure domain tests for the scheduling engine. No Spring, no database — these
 * run in the standard {@code test} phase locally (PRD-NFR-006 covers the
 * DB-side behaviour separately).
 */
class DirectAvailabilitySearchTest {

	private static final Instant NINE = Instant.parse("2026-09-01T09:00:00Z");
	private static final Duration ONE_HOUR = Duration.ofHours(1);

	private final DirectAvailabilitySearch engine = new DirectAvailabilitySearch();

	// --- capability / qualification
	// -----------------------------------------------------

	@Test
	@DisplayName("PRD-SVC-006 / SCH-AT-001: a staff member with the required valid qualification is selected")
	void qualifiedStaffIsSelected() {
		RequirementSpec staffReq = anyStaffRequiring(CapabilityRequirement.of("PHYSIO"));
		ResourceCandidate qualified = staff("s1", List.of(CapabilitySpec.of("PHYSIO")), fullDay());

		List<CandidateOption> options = engine
				.search(snapshot(oneHourWindow(), staffReq, Map.of("r-staff", List.of(qualified))));

		assertThat(options).isNotEmpty();
		assertThat(options.get(0).assignments()).extracting(AssignmentChoice::resourceId).containsExactly("s1");
	}

	@Test
	@DisplayName("PRD-SVC-006 / SCH-AT-002: a staff member without the required qualification is rejected")
	void unqualifiedStaffIsRejected() {
		RequirementSpec staffReq = anyStaffRequiring(CapabilityRequirement.of("PHYSIO"));
		ResourceCandidate wrongSkill = staff("s1", List.of(CapabilitySpec.of("MASSAGE")), fullDay());

		List<CandidateOption> options = engine
				.search(snapshot(oneHourWindow(), staffReq, Map.of("r-staff", List.of(wrongSkill))));

		assertThat(options).isEmpty();
	}

	@Test
	@DisplayName("PRD-SVC-006 / SCH-AT-002: a staff member whose qualification has expired is rejected")
	void expiredQualificationIsRejected() {
		RequirementSpec staffReq = anyStaffRequiring(CapabilityRequirement.of("PHYSIO"));
		CapabilitySpec expired = new CapabilitySpec("PHYSIO", null, null, LocalDate.of(2026, 8, 31));
		ResourceCandidate staff = staff("s1", List.of(expired), fullDay());

		List<CandidateOption> options = engine
				.search(snapshot(oneHourWindow(), staffReq, Map.of("r-staff", List.of(staff))));

		assertThat(options).isEmpty();
	}

	@Test
	@DisplayName("PRD-SVC-006: a minimum level requirement excludes under-qualified staff")
	void minimumLevelIsEnforced() {
		RequirementSpec staffReq = anyStaffRequiring(new CapabilityRequirement("PHYSIO", 3));
		ResourceCandidate tooJunior = staff("s1", List.of(new CapabilitySpec("PHYSIO", 2, null, null)), fullDay());
		ResourceCandidate senior = staff("s2", List.of(new CapabilitySpec("PHYSIO", 3, null, null)), fullDay());

		List<CandidateOption> options = engine
				.search(snapshot(oneHourWindow(), staffReq, Map.of("r-staff", List.of(tooJunior, senior))));

		assertThat(options).allSatisfy(
				o -> assertThat(o.assignments()).extracting(AssignmentChoice::resourceId).containsExactly("s2"));
	}

	// --- rooms / generic resource model
	// -------------------------------------------------

	@Test
	@DisplayName("PRD-SVC-002 / RES-AT-002: a room requirement is matched by room capabilities")
	void roomCapabilitiesAreMatched() {
		RequirementSpec roomReq = new RequirementSpec("r-room", BaseKind.PLACE, true, SelectionMode.ANY,
				CapabilityRequirement.of("PRIVATE"), Set.of(), Set.of());
		ResourceCandidate privateRoom = place("room1", List.of(CapabilitySpec.of("PRIVATE")), fullDay());
		ResourceCandidate openRoom = place("room2", List.of(CapabilitySpec.of("OPEN")), fullDay());

		List<CandidateOption> options = engine
				.search(snapshot(oneHourWindow(), roomReq, Map.of("r-room", List.of(privateRoom, openRoom))));

		assertThat(options).isNotEmpty();
		assertThat(options).allSatisfy(
				o -> assertThat(o.assignments()).extracting(AssignmentChoice::resourceId).containsExactly("room1"));
	}

	@Test
	@DisplayName("PRD-RES-003 / RES-AT-001: a brand-new resource type is schedulable with no engine change")
	void newResourceTypeIsSchedulable() {
		// A novel ASSET type the engine has never heard of — it must not branch on the
		// type.
		RequirementSpec droneReq = new RequirementSpec("r-drone", BaseKind.ASSET, true, SelectionMode.ANY,
				CapabilityRequirement.of("AERIAL_SURVEY"), Set.of(), Set.of());
		ResourceCandidate drone = new ResourceCandidate("drone-9", BaseKind.ASSET, "type-inspection-drone",
				List.of(CapabilitySpec.of("AERIAL_SURVEY")), fullDay(), List.of(), List.of(), Set.of());

		List<CandidateOption> options = engine
				.search(snapshot(oneHourWindow(), droneReq, Map.of("r-drone", List.of(drone))));

		assertThat(options).isNotEmpty();
		assertThat(options.get(0).assignments()).extracting(AssignmentChoice::resourceId).containsExactly("drone-9");
	}

	// --- optional requirements & compatibility
	// ------------------------------------------

	@Test
	@DisplayName("PRD-SVC-003 / SVC-AT-001: an optional equipment requirement is included when available and skipped when not")
	void optionalEquipmentIsHandled() {
		RequirementSpec staffReq = anyStaffRequiring(CapabilityRequirement.of("PHYSIO"));
		RequirementSpec optionalEquip = new RequirementSpec("r-equip", BaseKind.ASSET, false, SelectionMode.ANY,
				CapabilityRequirement.of("ULTRASOUND"), Set.of(), Set.of());
		ResourceCandidate staff = staff("s1", List.of(CapabilitySpec.of("PHYSIO")), fullDay());
		ResourceCandidate equip = new ResourceCandidate("e1", BaseKind.ASSET, "type-ultrasound",
				List.of(CapabilitySpec.of("ULTRASOUND")), fullDay(), List.of(), List.of(), Set.of());

		// Present: equipment is included.
		List<CandidateOption> withEquip = engine.search(snapshot(oneHourWindow(), List.of(staffReq, optionalEquip),
				Map.of("r-staff", List.of(staff), "r-equip", List.of(equip))));
		assertThat(withEquip.get(0).assignments()).extracting(AssignmentChoice::resourceId)
				.containsExactlyInAnyOrder("s1", "e1");

		// Absent: option is still feasible, without the optional equipment.
		List<CandidateOption> withoutEquip = engine.search(snapshot(oneHourWindow(), List.of(staffReq, optionalEquip),
				Map.of("r-staff", List.of(staff), "r-equip", List.<ResourceCandidate>of())));
		assertThat(withoutEquip).isNotEmpty();
		assertThat(withoutEquip.get(0).assignments()).extracting(AssignmentChoice::resourceId).containsExactly("s1");
	}

	@Test
	@DisplayName("PRD-RES-006/008 / RES-AT-004: fixed equipment is constrained to its compatible room")
	void fixedEquipmentConstrainedToCompatibleRoom() {
		RequirementSpec roomReq = new RequirementSpec("r-room", BaseKind.PLACE, true, SelectionMode.ANY, null, Set.of(),
				Set.of());
		RequirementSpec equipReq = new RequirementSpec("r-equip", BaseKind.ASSET, true, SelectionMode.ANY, null,
				Set.of(), Set.of());
		ResourceCandidate roomA = place("roomA", List.of(), fullDay());
		ResourceCandidate roomB = place("roomB", List.of(), fullDay());
		// Fixed equipment usable only in roomA.
		ResourceCandidate fixedEquip = new ResourceCandidate("eq", BaseKind.ASSET, "type-fixed", List.of(), fullDay(),
				List.of(), List.of(), Set.of("roomA"));

		List<CandidateOption> options = engine.search(snapshot(oneHourWindow(), List.of(roomReq, equipReq),
				Map.of("r-room", List.of(roomA, roomB), "r-equip", List.of(fixedEquip))));

		assertThat(options).isNotEmpty();
		assertThat(options).allSatisfy(o -> assertThat(o.assignments()).extracting(AssignmentChoice::resourceId)
				.contains("roomA").doesNotContain("roomB"));
	}

	// --- selection mode
	// -----------------------------------------------------------------

	@Test
	@DisplayName("PRD-SVC-005 / SCH-AT-003: a REQUIRED specific staff member is enforced as a hard constraint")
	void requiredStaffIsEnforced() {
		RequirementSpec requireS1 = new RequirementSpec("r-staff", BaseKind.PERSON, true, SelectionMode.REQUIRED,
				CapabilityRequirement.of("PHYSIO"), Set.of("s1"), Set.of());
		ResourceCandidate s1 = staff("s1", List.of(CapabilitySpec.of("PHYSIO")), fullDay());
		ResourceCandidate s2 = staff("s2", List.of(CapabilitySpec.of("PHYSIO")), fullDay());

		// s2 is equally qualified but not permitted; only s1 may be assigned.
		List<CandidateOption> options = engine
				.search(snapshot(oneHourWindow(), requireS1, Map.of("r-staff", List.of(s1, s2))));
		assertThat(options).isNotEmpty();
		assertThat(options).allSatisfy(
				o -> assertThat(o.assignments()).extracting(AssignmentChoice::resourceId).containsExactly("s1"));

		// If the required s1 is unavailable, there is no option even though s2 is
		// qualified.
		ResourceCandidate s1Busy = staff("s1", List.of(CapabilitySpec.of("PHYSIO")), fullDay(), List.of(),
				List.of(fullSlot()));
		List<CandidateOption> none = engine
				.search(snapshot(oneHourWindow(), requireS1, Map.of("r-staff", List.of(s1Busy, s2))));
		assertThat(none).isEmpty();
	}

	@Test
	@DisplayName("PRD-SVC-005 / SCH-AT-004: a PREFERRED staff member is ranked higher but not mandatory")
	void preferredStaffIsRankedHigher() {
		RequirementSpec preferS2 = new RequirementSpec("r-staff", BaseKind.PERSON, true, SelectionMode.PREFERRED,
				CapabilityRequirement.of("PHYSIO"), Set.of(), Set.of("s2"));
		ResourceCandidate s1 = staff("s1", List.of(CapabilitySpec.of("PHYSIO")), fullDay());
		ResourceCandidate s2 = staff("s2", List.of(CapabilitySpec.of("PHYSIO")), fullDay());

		List<CandidateOption> options = engine
				.search(snapshot(oneHourWindow(), preferS2, Map.of("r-staff", List.of(s1, s2))));

		// Highest-ranked option uses the preferred staff member.
		assertThat(options).isNotEmpty();
		assertThat(options.get(0).assignments()).extracting(AssignmentChoice::resourceId).containsExactly("s2");
		assertThat(options.get(0).score()).isGreaterThan(0.0);
	}

	// --- availability / reservations
	// ----------------------------------------------------

	@Test
	@DisplayName("PRD-AVL-003 / AVL-AT-001: availability = rules ∩ ¬blocks ∩ ¬reservations")
	void availabilityIsIntersectionOfRulesBlocksAndReservations() {
		RequirementSpec staffReq = anyStaffRequiring(CapabilityRequirement.of("PHYSIO"));
		// Available 09:00-12:00, blocked 09:00-10:00, reserved 11:00-12:00 → only the
		// 10:00 slot is free.
		ResourceCandidate staff = staff("s1", List.of(CapabilitySpec.of("PHYSIO")),
				List.of(new Interval(NINE, NINE.plus(Duration.ofHours(3)))),
				List.of(new Interval(NINE, NINE.plus(ONE_HOUR))),
				List.of(new Interval(NINE.plus(Duration.ofHours(2)), NINE.plus(Duration.ofHours(3)))));

		List<CandidateOption> options = engine.search(snapshot(new Interval(NINE, NINE.plus(Duration.ofHours(3))),
				staffReq, Map.of("r-staff", List.of(staff))));

		assertThat(options).extracting(o -> o.slot().start()).containsExactly(NINE.plus(ONE_HOUR)); // only the
																									// 10:00-11:00 slot
																									// survives
	}

	@Test
	@DisplayName("PRD-RSV-002/004 / RSV-AT-001: a reserved resource is excluded from options at the conflicting time")
	void reservedResourceIsExcluded() {
		RequirementSpec staffReq = anyStaffRequiring(CapabilityRequirement.of("PHYSIO"));
		ResourceCandidate reserved = staff("s1", List.of(CapabilitySpec.of("PHYSIO")), fullDay(), List.of(),
				List.of(fullSlot()));

		List<CandidateOption> options = engine
				.search(snapshot(oneHourWindow(), staffReq, Map.of("r-staff", List.of(reserved))));

		assertThat(options).isEmpty();
	}

	@Test
	@DisplayName("PRD-SCH-004 / BKG-AT-006: direct search never reassigns an existing booking's resource")
	void directSearchNeverReassignsExistingReservation() {
		// The only qualified staff member is already reserved for the whole window.
		// Search must
		// return no option — it treats the reservation as blocking, it does not
		// free/reassign it.
		RequirementSpec staffReq = anyStaffRequiring(CapabilityRequirement.of("PHYSIO"));
		ResourceCandidate onlyStaffBusy = staff("s1", List.of(CapabilitySpec.of("PHYSIO")), fullDay(), List.of(),
				List.of(fullSlot()));

		List<CandidateOption> options = engine
				.search(snapshot(oneHourWindow(), staffReq, Map.of("r-staff", List.of(onlyStaffBusy))));

		assertThat(options).isEmpty();
	}

	// --- statelessness
	// ------------------------------------------------------------------

	@Test
	@DisplayName("PRD-SCH-001 / SCH-AT-005: the engine is stateless — results depend only on the snapshot")
	void engineIsStateless() {
		// The engine holds no non-constant instance state.
		assertThat(DirectAvailabilitySearch.class.getDeclaredFields())
				.allSatisfy(f -> assertThat(java.lang.reflect.Modifier.isStatic(f.getModifiers())).isTrue());

		RequirementSpec staffReq = anyStaffRequiring(CapabilityRequirement.of("PHYSIO"));
		SchedulingSnapshot a = snapshot(oneHourWindow(), staffReq,
				Map.of("r-staff", List.of(staff("s1", List.of(CapabilitySpec.of("PHYSIO")), fullDay()))));
		SchedulingSnapshot b = snapshot(oneHourWindow(), staffReq, Map.of("r-staff", List.<ResourceCandidate>of()));

		List<CandidateOption> firstA = engine.search(a);
		engine.search(b); // interleave a different snapshot
		List<CandidateOption> secondA = engine.search(a);

		assertThat(secondA).isEqualTo(firstA); // no carry-over between invocations
	}

	// --- fixtures
	// -----------------------------------------------------------------------

	@Test
	@DisplayName("PRD-RES-012 / RES-AT-005: a resource setup buffer excludes a slot too close to a prior reservation")
	void setupBufferExcludesSlotTooClose() {
		RequirementSpec staffReq = anyStaffRequiring(CapabilityRequirement.of("PHYSIO"));
		Interval priorReservation = new Interval(NINE.plus(ONE_HOUR), NINE.plus(Duration.ofHours(2))); // 10:00–11:00
		Interval window = new Interval(NINE.plus(Duration.ofHours(2)), NINE.plus(Duration.ofHours(3))); // slot
																										// 11:00–12:00

		// With a 30-min setup, the slot's buffered window [10:30,12:00] overlaps the
		// prior reservation.
		List<CandidateOption> buffered = engine.search(snapshot(window, staffReq,
				Map.of("r-staff", List.of(staffWithBuffer("s1", 30, 0, List.of(priorReservation))))));
		assertThat(buffered).isEmpty();

		// With no setup, the same slot [11:00,12:00] is adjacent (not overlapping) →
		// feasible.
		List<CandidateOption> unbuffered = engine.search(snapshot(window, staffReq,
				Map.of("r-staff", List.of(staffWithBuffer("s1", 0, 0, List.of(priorReservation))))));
		assertThat(unbuffered).isNotEmpty();
	}

	@Test
	@DisplayName("PRD-RES-012 / RES-AT-005: a resource cleanup buffer excludes a slot ending too close to a later reservation")
	void cleanupBufferExcludesSlotTooClose() {
		RequirementSpec staffReq = anyStaffRequiring(CapabilityRequirement.of("PHYSIO"));
		Interval laterReservation = new Interval(NINE.plus(Duration.ofHours(2)), NINE.plus(Duration.ofHours(3))); // 11:00–12:00
		Interval window = new Interval(NINE.plus(ONE_HOUR), NINE.plus(Duration.ofHours(2))); // slot 10:00–11:00

		List<CandidateOption> buffered = engine.search(snapshot(window, staffReq,
				Map.of("r-staff", List.of(staffWithBuffer("s1", 0, 30, List.of(laterReservation))))));
		assertThat(buffered).isEmpty();

		List<CandidateOption> unbuffered = engine.search(snapshot(window, staffReq,
				Map.of("r-staff", List.of(staffWithBuffer("s1", 0, 0, List.of(laterReservation))))));
		assertThat(unbuffered).isNotEmpty();
	}

	@Test
	@DisplayName("PRD-SVC-009 / RES-AT-005: a service lead time excludes a slot whose prep overlaps a prior reservation")
	void serviceLeadTimeExcludesSlot() {
		RequirementSpec staffReq = anyStaffRequiring(CapabilityRequirement.of("PHYSIO"));
		Interval priorReservation = new Interval(NINE, NINE.plus(ONE_HOUR)); // 09:00–10:00
		Interval window = new Interval(NINE.plus(ONE_HOUR), NINE.plus(Duration.ofHours(2))); // slot 10:00–11:00
		ResourceCandidate staff = staffWithBuffer("s1", 0, 0, List.of(priorReservation));

		// 30-min lead → buffered window [09:30,11:00] overlaps the prior reservation →
		// infeasible.
		SchedulingSnapshot withLead = new SchedulingSnapshot(window, ONE_HOUR, List.of(staffReq),
				Map.of("r-staff", List.of(staff)), SearchParameters.defaults(), 30);
		assertThat(engine.search(withLead)).isEmpty();

		// No lead → slot [10:00,11:00] is adjacent to the reservation → feasible.
		assertThat(engine.search(snapshot(window, staffReq, Map.of("r-staff", List.of(staff))))).isNotEmpty();
	}

	private static ResourceCandidate staffWithBuffer(String id, int setup, int cleanup, List<Interval> reservations) {
		return new ResourceCandidate(id, BaseKind.PERSON, "type-staff", List.of(CapabilitySpec.of("PHYSIO")), fullDay(),
				List.of(), reservations, Set.of(), setup, cleanup);
	}

	private static Interval oneHourWindow() {
		return new Interval(NINE, NINE.plus(ONE_HOUR)); // exactly one 1-hour slot
	}

	private static Interval fullSlot() {
		return new Interval(NINE, NINE.plus(ONE_HOUR));
	}

	private static List<Interval> fullDay() {
		return List.of(new Interval(NINE, NINE.plus(Duration.ofHours(9))));
	}

	private static RequirementSpec anyStaffRequiring(CapabilityRequirement capability) {
		return new RequirementSpec("r-staff", BaseKind.PERSON, true, SelectionMode.ANY, capability, Set.of(), Set.of());
	}

	private static ResourceCandidate staff(String id, List<CapabilitySpec> caps, List<Interval> availability) {
		return staff(id, caps, availability, List.of(), List.of());
	}

	private static ResourceCandidate staff(String id, List<CapabilitySpec> caps, List<Interval> availability,
			List<Interval> blocked, List<Interval> reservations) {
		return new ResourceCandidate(id, BaseKind.PERSON, "type-staff", caps, availability, blocked, reservations,
				Set.of());
	}

	private static ResourceCandidate place(String id, List<CapabilitySpec> caps, List<Interval> availability) {
		return new ResourceCandidate(id, BaseKind.PLACE, "type-room", caps, availability, List.of(), List.of(),
				Set.of());
	}

	private static SchedulingSnapshot snapshot(Interval window, RequirementSpec requirement,
			Map<String, List<ResourceCandidate>> candidates) {
		return snapshot(window, List.of(requirement), candidates);
	}

	private static SchedulingSnapshot snapshot(Interval window, List<RequirementSpec> requirements,
			Map<String, List<ResourceCandidate>> candidates) {
		return new SchedulingSnapshot(window, ONE_HOUR, requirements, candidates, SearchParameters.defaults());
	}
}
