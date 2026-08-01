# 01 — Product Requirements Document (PRD)

**Status:** Living document. Requirement IDs are stable and must never be renamed or
reused. Superseded requirements are retained with a status marker and a forward link.

**Requirement ID convention:** `PRD-<GROUP>-<NNN>` (zero-padded, sequential per group).
**Acceptance test ID convention:** `<GROUP>-AT-<NNN>`.

**Requirement groups:**

| Group | Domain |
|-------|--------|
| `TEN` | Tenancy & isolation |
| `IDN` | Identity (Firebase → ApplicationUser) |
| `MEM` | Membership, roles, permissions |
| `RES` | Resources, types, capabilities, mobility, compatibility |
| `SVC` | Services, requirements, constraints |
| `AVL` | Availability rules & blocks |
| `SCH` | Scheduling engine / candidate search |
| `SUB` | Booking subject |
| `BKG` | Booking lifecycle & channels |
| `ASN` | Assignments & policies |
| `RSV` | Reservations & transactional integrity |
| `AUD` | Audit |
| `SEC` | Cross-cutting authorization/security |
| `NFR` | Non-functional |

**Requirement status values:** `PROPOSED`, `ACCEPTED`, `DEFERRED`, `SUPERSEDED`, `REMOVED`.
Most requirements below are `ACCEPTED` (design accepted) with implementation tracked in
`docs/08-REQUIREMENTS-TRACEABILITY.md`. Items explicitly out of the initial slice are
`DEFERRED`.

---

## 1. Product vision

See `docs/00-VISION.md`. In short: a multi-tenant platform where organisations define
bookable services as **requirements over generic resources** (people, places, assets),
and the platform allocates a valid, non-conflicting combination at a valid time —
internal-first, self-service-ready.

## 2. Problem statement

Existing tools hardcode one resource shape and one set of constraints, turning every new
resource type or rule into schema + engine changes. Multi-resource services (staff **and**
room **and** equipment, with qualifications, compatibilities, movement/setup times, and
swappable assignments) are poorly served. Multi-tenant isolation and transactional
double-booking prevention are frequently bolted on late and unsafely.

## 3. Target users

- Organisations delivering appointment/session services combining staff, spaces, equipment.
- Internal schedulers and receptionists (initial delivery).
- Self-service customers (future channel).
- Resource members (staff) whose availability and qualifications drive scheduling.
- Organisation administrators configuring types, services, resources and membership.

## 4. Personas

- **Priya — Organisation Administrator.** Sets up resource types, services, resources,
  capabilities, locations and membership. Needs safe defaults and clear configuration.
- **Sam — Scheduler / Receptionist.** Creates bookings for subjects, searches
  availability, confirms bookings. Needs fast, correct candidate options and clear
  conflict feedback.
- **Ravi — Resource Member (e.g. physiotherapist).** A staff `Resource`; may also be an
  `OrganisationMember` with a login, but not necessarily. Availability and qualifications
  affect scheduling.
- **Alex — Booking Subject (e.g. patient).** Receives the service. Not necessarily a
  system user in the initial release.
- **Jordan — Viewer/Auditor.** Read-only visibility and audit review.

## 5. User journeys (initial, internal)

1. **Configure organisation** — Priya creates a tenant's locations, resource types
   (staff/room/equipment), resources with capabilities, and a service type with
   requirements.
2. **Search availability** — Sam selects a service, a time window and a subject; the
   system returns ranked candidate option sets (which resources satisfy each requirement,
   at which times), applying hard constraints and ranking by soft constraints.
3. **Confirm booking** — Sam confirms a chosen option; the system atomically creates the
   booking, assignments and one reservation per exclusive resource, or fails cleanly with
   a conflict if a resource became unavailable.
4. **Manage booking** — Sam cancels/completes/marks no-show; audit records the action.

## 6. Functional scope (initial) vs non-goals

**In scope (initial):** internal authenticated booking; multi-tenancy; generic resources
with capabilities; services with required/optional requirements; direct availability
search; atomic confirmation with reservations; assignments separate from bookings;
cross-tenant isolation; audit of key actions.

**Non-goals (initial):** public self-service UI; schedule repair/reassignment during
search; whole-schedule optimisation; booking holds; capacity/pooled beyond model shape;
CRM; Redis/microservices/Kafka/CQRS/event-sourcing/rules-engine.

---

## 7. Functional requirements

### 7.1 Tenancy (`TEN`)

- **PRD-TEN-001** *(ACCEPTED)* A `Tenant` represents an organisation and is the root of
  all tenant-owned data.
- **PRD-TEN-002** *(ACCEPTED)* All tenant-owned data carries `tenant_id` (locations,
  memberships, resource types, resources, capabilities, attribute definitions, services,
  requirements, availability, bookings, assignments, reservations, audit records).
- **PRD-TEN-003** *(ACCEPTED)* Repository methods that access tenant-owned data must be
  explicitly tenant-scoped, e.g. `findById(TenantId, ResourceId)`. Unscoped
  `findById(ResourceId)` for tenant-owned data is prohibited.
- **PRD-TEN-004** *(ACCEPTED)* The system must never trust a client-supplied tenant id
  without validating that the authenticated user belongs to that organisation.
- **PRD-TEN-005** *(ACCEPTED)* Tenant-scoped uniqueness constraints must generally include
  `tenant_id`.
- **PRD-TEN-006** *(ACCEPTED)* Where practical, foreign keys include the tenant
  discriminator so cross-tenant references are prevented at database level.
- **PRD-TEN-007** *(ACCEPTED)* Cross-tenant isolation must be covered by integration and
  acceptance tests. *(AT: TEN-AT-001, TEN-AT-002)*
- **PRD-TEN-008** *(DEFERRED)* PostgreSQL Row-Level Security may be added as defence in
  depth; it must not replace explicit application-level tenant scoping. → see
  `07-OPEN-QUESTIONS.md` OQ-TEN-1.

### 7.2 Identity (`IDN`)

- **PRD-IDN-001** *(ACCEPTED)* Firebase Authentication establishes global user identity.
  The backend verifies Firebase ID tokens; it does not manage passwords.
- **PRD-IDN-002** *(ACCEPTED)* PostgreSQL owns the `ApplicationUser` (linked to a Firebase
  UID), organisation membership, active tenant, roles and permissions.
- **PRD-IDN-003** *(ACCEPTED)* An `ApplicationUser` (system login) and a person `Resource`
  (schedulable staff) are **distinct** concepts and may exist independently.
- **PRD-IDN-004** *(ACCEPTED)* A user may belong to multiple organisations; the request
  must resolve exactly one **active tenant**, validated against membership.

### 7.3 Membership, roles & permissions (`MEM`)

- **PRD-MEM-001** *(ACCEPTED)* An `OrganisationMember` links an `ApplicationUser` to a
  `Tenant` with one or more `Role`s.
- **PRD-MEM-002** *(ACCEPTED)* Initial roles: Organisation Administrator, Scheduler,
  Resource Member, Viewer.
- **PRD-MEM-003** *(ACCEPTED)* Authorization is by **permission**, not by hardcoded
  role-name checks. Permissions include: resource management, service management,
  availability management, booking creation, booking viewing, booking cancellation,
  booking rescheduling, booking status update (complete/no-show), organisation membership
  management.
- **PRD-MEM-004** *(ACCEPTED)* Membership must be validated on every tenant-scoped action;
  actions the member's permissions do not grant are rejected. *(AT: MEM-AT-001)*

### 7.4 Resources (`RES`)

- **PRD-RES-001** *(ACCEPTED)* Every schedulable item is a generic `Resource`. There must
  be no separate hardcoded scheduling models for staff, rooms and equipment.
- **PRD-RES-002** *(ACCEPTED)* Each `Resource` has a `ResourceType`; each type carries a
  controlled `BaseResourceKind` ∈ {PERSON, PLACE, ASSET}.
- **PRD-RES-003** *(ACCEPTED)* Initial concrete types: staff member (PERSON), room (PLACE),
  equipment (ASSET). Adding a new type must not require changing the scheduling engine.
  *(AT: RES-AT-001)*
- **PRD-RES-004** *(ACCEPTED)* Resources expose `ResourceCapability` records (e.g.
  qualification, skill level, accessibility, room privacy, equipment category, capacity,
  certification expiry, compatibility).
- **PRD-RES-005** *(ACCEPTED)* Type-specific structural data may be modelled as a concrete
  **profile via composition**; common scheduling behaviour remains on the generic model.
  Large inheritance hierarchies are prohibited.
- **PRD-RES-006** *(ACCEPTED)* Equipment supports `EquipmentMobility` ∈ {FIXED, MOVABLE,
  POOLED}. FIXED equipment may be permanently associated with a room. *(AT: RES-AT-004)*
- **PRD-RES-007** *(ACCEPTED — model; DEFERRED implementation beyond slice)* MOVABLE
  equipment may model current/home location, compatible destination rooms, movement time,
  setup time and cleanup time. POOLED equipment is selected from an interchangeable set.
  The first implementation may support only the subset the vertical slice needs.
- **PRD-RES-008** *(ACCEPTED)* `ResourceCompatibility` expresses which resources may (or
  may not) be combined (e.g. equipment usable in a given room).
- **PRD-RES-009** *(ACCEPTED)* `AttributeDefinition` allows tenant-defined structured
  attributes on resources/capabilities without a dynamic meta-model or rules engine.

### 7.5 Services & requirements (`SVC`)

- **PRD-SVC-001** *(ACCEPTED)* A `ServiceType` defines a bookable service and its one or
  more `ResourceRequirement` records.
- **PRD-SVC-002** *(ACCEPTED)* A `ResourceRequirement` describes **what** is needed
  (kind/type + required capabilities), not a specific named resource — except where a
  booking request explicitly requires one.
- **PRD-SVC-003** *(ACCEPTED)* A requirement may be **required** (must be satisfied for
  confirmation) or **optional**. *(AT: SVC-AT-001)*
- **PRD-SVC-004** *(ACCEPTED)* A requirement carries `RequirementConstraint`s classified as
  **hard** (validity) or **soft** (ranking). Safety, qualification, compatibility and
  availability rules are **hard by default**.
- **PRD-SVC-005** *(ACCEPTED)* Staff selection mode per staff requirement/booking request:
  `REQUIRED` (specific staff — hard constraint), `PREFERRED` (rank higher — soft
  constraint), `ANY` (any suitably qualified). *(AT: SCH-AT-003, SCH-AT-004)*
- **PRD-SVC-006** *(ACCEPTED)* Qualification matching supports: required qualification,
  optional minimum level, optional expiry date, and validity on the appointment date.
  *(AT: SCH-AT-001, SCH-AT-002)*
- **PRD-SVC-007** *(DEFERRED)* Complex supervision rules and qualification alternatives.
  → `06-FUTURE-IDEAS.md`.

### 7.6 Availability (`AVL`)

- **PRD-AVL-001** *(ACCEPTED)* Resources have `AvailabilityRule`s defining when they are
  available.
- **PRD-AVL-002** *(ACCEPTED)* `BlockedAvailability` overrides rules to mark a resource
  unavailable for a period.
- **PRD-AVL-003** *(ACCEPTED)* Effective availability is the intersection of a resource's
  availability with the absence of blocks and the absence of conflicting reservations.
  *(AT: AVL-AT-001)*

### 7.7 Scheduling / candidate search (`SCH`)

- **PRD-SCH-001** *(ACCEPTED)* The scheduling engine computes candidate options **in
  memory** from an **immutable snapshot** and **must not** mutate bookings, assignments,
  reservations or hold authoritative calendar state. *(AT: SCH-AT-005)*
- **PRD-SCH-002** *(ACCEPTED)* **Hard constraints** determine validity: resource
  availability, required qualification, qualification validity, room compatibility,
  equipment compatibility, location, reservation conflicts.
- **PRD-SCH-003** *(ACCEPTED)* **Soft constraints** rank otherwise-valid options: preferred
  practitioner, preferred room, continuity of care, reduced equipment movement, reduced
  timetable gaps, workload balancing.
- **PRD-SCH-004** *(ACCEPTED)* **Direct availability search** finds options **without
  changing** any existing booking or assignment. It never silently reassigns.
  *(AT: BKG-AT-006)*
- **PRD-SCH-005** *(ACCEPTED)* Initial search is deliberately simple: direct availability
  only; limited search horizon; fixed search increments (if useful); limited result count.
  → defaults recorded in `07-OPEN-QUESTIONS.md` OQ-SCH-1.
- **PRD-SCH-006** *(ACCEPTED)* Setup/cleanup/movement buffers, where modelled, extend the
  time a resource is considered consumed for feasibility. *(AT: RES-AT-005)*
- **PRD-SCH-007** *(DEFERRED)* **Schedule repair** — proposal-only, explicit scheduler
  approval, no appointment-time changes, same location only, staff reassignment only,
  max one affected existing booking, locked assignments untouched. Must not be built in
  the initial slice unless explicitly requested. → `06-FUTURE-IDEAS.md`, ADR-003 §future.
- **PRD-SCH-008** *(DEFERRED)* Whole-schedule optimisation. Out of scope for initial release.

### 7.8 Booking subject (`SUB`)

- **PRD-SUB-001** *(ACCEPTED)* Every booking has a `BookingSubject` representing the
  person, organisation or asset **receiving** the service. It must not be named `Customer`
  in the core domain.
- **PRD-SUB-002** *(ACCEPTED)* Initial `BookingSubject` fields: type, display name, email,
  phone, external reference. The product must not become a CRM.
- **PRD-SUB-003** *(ACCEPTED)* The subject is distinct from the authenticated actor, the
  organisation member, and the assigned staff resource.

### 7.9 Bookings & channels (`BKG`)

- **PRD-BKG-001** *(ACCEPTED)* A `Booking` is a commitment to provide a configured service
  at a given time. It must **not** contain `staffId`, `roomId`, `equipmentId` or similar
  hardcoded resource fields. *(AT: BKG-AT-005)*
- **PRD-BKG-002** *(ACCEPTED)* Booking status lifecycle initially: `CONFIRMED`,
  `CANCELLED`, `COMPLETED`, `NO_SHOW`. Temporary holds are deferred.
- **PRD-BKG-003** *(ACCEPTED)* A booking records its `channel` ∈ {INTERNAL, SELF_SERVICE,
  API, IMPORT}. Only INTERNAL is delivered initially.
- **PRD-BKG-004** *(ACCEPTED)* Booking confirmation atomically creates the booking, all
  required assignments and every required reservation, or fails without creating a partial
  booking. *(AT: BKG-AT-004)*
- **PRD-BKG-005** *(ACCEPTED)* A booking is confirmable only when fully feasible: every
  hard resource requirement has a valid assignment. Partially assigned confirmed bookings
  are unsupported initially. *(AT: BKG-AT-004)*
- **PRD-BKG-006** *(ACCEPTED)* Confirmation revalidates availability inside the database
  transaction; if a slot became unavailable, the API returns a clear **conflict** response.
  *(AT: BKG-AT-004, RSV-AT-002)*
- **PRD-BKG-007** *(DEFERRED)* Booking holds (temporary reservations pre-confirmation),
  required by self-service. → `06-FUTURE-IDEAS.md`.
- **PRD-BKG-008** *(ACCEPTED)* A `CONFIRMED` booking may be **cancelled** by a user with the
  `BOOKING_CANCEL` permission. Cancellation sets status `CANCELLED` and **releases all of the
  booking's reservations**, so the freed slot becomes bookable again. Only a `CONFIRMED`
  booking may be cancelled (invalid transitions are rejected). *(AT: BKG-AT-007)*
- **PRD-BKG-009** *(ACCEPTED)* A `CONFIRMED` booking may be marked **`COMPLETED`** or
  **`NO_SHOW`** by a user with the `BOOKING_UPDATE` permission. These are terminal outcome
  states and do not change reservations. Only a `CONFIRMED` booking may transition. *(AT: BKG-AT-008)*
- **PRD-BKG-010** *(ACCEPTED)* A booking can be **retrieved by id** (tenant-scoped), including
  its assignments, by a user with the `BOOKING_VIEW` permission; unknown ids return not-found.
  *(AT: BKG-AT-009)*
- **PRD-BKG-011** *(ACCEPTED)* Bookings for the active tenant can be **listed** (optionally
  filtered by status) by a user with `BOOKING_VIEW`. Only the active tenant's bookings are
  returned (PRD-TEN-002/003). *(AT: BKG-AT-010)*
- **PRD-BKG-012** *(ACCEPTED)* A `CONFIRMED` booking may be **rescheduled** to a new time by a
  user with the `BOOKING_RESCHEDULE` permission. The booking **keeps its identity**
  (PRD-ASN-002); its existing reservations are released and new reservations created
  **atomically** — a conflict at the new time returns a conflict response and leaves the
  booking unchanged (PRD-RSV-004). *(AT: BKG-AT-011)*

### 7.10 Assignments & policies (`ASN`)

- **PRD-ASN-001** *(ACCEPTED)* A `ResourceAssignment` identifies the resource currently
  selected to satisfy a requirement. Assignments are confirmed when the booking is
  confirmed.
- **PRD-ASN-002** *(ACCEPTED)* An assignment may change later **without** changing the
  identity of the booking.
- **PRD-ASN-003** *(ACCEPTED — model; DEFERRED behaviour)* `AssignmentPolicy` supports
  assignments later becoming reassignable, explicitly locked, or locked by policy.
- **PRD-ASN-004** *(ACCEPTED)* Standard availability search must never silently change an
  existing assignment. *(AT: BKG-AT-006)*

### 7.11 Reservations & transactional integrity (`RSV`)

- **PRD-RSV-001** *(ACCEPTED)* A `Reservation` prevents an assigned exclusive resource from
  being consumed by conflicting bookings.
- **PRD-RSV-002** *(ACCEPTED)* Resources are **exclusive by default**: a resource cannot
  normally serve more than one booking at the same time. Staff cannot serve multiple
  simultaneous bookings initially. Exclusivity must not be weakened globally to add
  capacity. *(AT: RSV-AT-001)*
- **PRD-RSV-003** *(ACCEPTED)* Every confirmed booking creates one reservation for every
  consumed exclusive resource. *(AT: BKG-AT-004)*
- **PRD-RSV-004** *(ACCEPTED)* PostgreSQL must enforce protection against overlapping
  exclusive reservations (range types + exclusion constraint). Concurrent attempts to
  reserve the same resource result in only one successful booking. Application-only
  check-then-insert is prohibited. *(AT: RSV-AT-001, RSV-AT-002)*
- **PRD-RSV-005** *(ACCEPTED)* Availability search is advisory; a searched slot may become
  unavailable before confirmation, which the confirmation API surfaces as a conflict.
- **PRD-RSV-006** *(DEFERRED)* Capacity-based (non-exclusive) resources via an explicit
  capacity model. → `06-FUTURE-IDEAS.md`.

### 7.12 Audit (`AUD`)

- **PRD-AUD-001** *(ACCEPTED)* Key actions (booking create/cancel/complete/no-show,
  membership and configuration changes) produce tenant-scoped `AuditEvent` records.

---

## 8. Non-functional requirements (`NFR`)

- **PRD-NFR-001** *(ACCEPTED)* The backend is **stateless** and horizontally scalable;
  no mutable authoritative calendar state in process memory. PostgreSQL is the authority.
- **PRD-NFR-002** *(ACCEPTED)* Deployable to **Google Cloud Run** with 12-factor
  configuration (env-driven), graceful startup/shutdown.
- **PRD-NFR-003** *(ACCEPTED)* Health, **liveness** and **readiness** endpoints exposed.
- **PRD-NFR-004** *(ACCEPTED)* **Structured (JSON) logging** including tenant and request
  correlation identifiers (never secrets/tokens).
- **PRD-NFR-005** *(ACCEPTED)* **Database migrations** (Flyway) are the only mechanism for
  schema change; every schema change ships as a migration and is tested.
- **PRD-NFR-006** *(ACCEPTED)* Integration tests for transactional/DB behaviour use real
  PostgreSQL via **Testcontainers**; not mocked repositories.
- **PRD-NFR-007** *(ACCEPTED)* **Architecture rules** (module boundaries; scheduling
  domain free of HTTP/Spring MVC/Firebase/JPA) are enforced by automated tests (ArchUnit).
- **PRD-NFR-008** *(ACCEPTED)* **CI** builds, tests and runs documentation validation on
  every change.
- **PRD-NFR-009** *(ACCEPTED)* Current supported Java (**Java 21 LTS**) and Spring Boot 3.x.

## 9. Security requirements (`SEC`)

- **PRD-SEC-001** *(ACCEPTED)* Every request is authenticated via a verified Firebase ID
  token before any tenant-scoped action.
- **PRD-SEC-002** *(ACCEPTED)* Active tenant is derived from validated membership, never
  trusted from the client. *(AT: TEN-AT-002)*
- **PRD-SEC-003** *(ACCEPTED)* Authorization is permission-based (see PRD-MEM-003); denied
  actions return an authorization error and are auditable.
- **PRD-SEC-004** *(ACCEPTED)* No secrets/tokens/PII beyond necessity in logs.

---

## 10. Multi-tenancy requirements

Consolidated in group `TEN` (§7.1) plus PRD-SEC-002. Cross-cutting rule: **isolation is
enforced in application code (explicit tenant scoping) and at the database (composite FKs,
tenant-scoped uniqueness), and verified by tests**; RLS is optional defence-in-depth.

## 11. Internal booking requirements

Consolidated in groups `SVC`, `AVL`, `SCH`, `SUB`, `BKG`, `ASN`, `RSV`. Only the
`INTERNAL` channel (PRD-BKG-003) is delivered initially.

## 12. Future self-service assumptions

Design must support (without redesign): published services, a public tenant identity/slug,
public availability search exposing **bookable options not internal calendars**, restricted
resource-detail exposure, customer-created bookings, holds, cancellation/rescheduling
policies, notifications, optional approval, optional resource selection. Do **not** assume
all bookings are internal, all services public, that public users see staff schedules, or
that all public bookings are immediately confirmed. No public UI is built now. See
`06-FUTURE-IDEAS.md` and ADR-005.

## 13. Default product policies

- Resources exclusive by default (PRD-RSV-002).
- Confirmed bookings fully feasible (PRD-BKG-005).
- Safety/qualification/compatibility/availability constraints hard by default (PRD-SVC-004).
- Direct search never reassigns (PRD-SCH-004, PRD-ASN-004).
- No holds, no schedule repair, no optimisation initially.

---

## 14. Acceptance criteria (mapped to tests)

Each user-facing criterion has an acceptance test ID; full mapping in
`docs/08-REQUIREMENTS-TRACEABILITY.md` and the plan in `docs/04-DECISION-REGISTER.md`
is cross-referenced. Initial acceptance criteria:

| AT ID | Criterion | Primary PRD |
|-------|-----------|-------------|
| TEN-AT-001 | A user of tenant A cannot read tenant B's data | PRD-TEN-002/003/007 |
| TEN-AT-002 | A client-supplied tenant id not matching membership is rejected | PRD-TEN-004, PRD-SEC-002 |
| MEM-AT-001 | An action without the required permission is rejected | PRD-MEM-004 |
| RES-AT-001 | A new resource type is schedulable with no engine change | PRD-RES-003 |
| SCH-AT-001 | A staff member with the required valid qualification is selected | PRD-SVC-006 |
| SCH-AT-002 | A staff member whose qualification is missing/expired is rejected | PRD-SVC-006 |
| SCH-AT-003 | `REQUIRED` specific staff is enforced as a hard constraint | PRD-SVC-005 |
| SCH-AT-004 | `PREFERRED` staff is ranked higher but not mandatory | PRD-SVC-005 |
| RES-AT-002 | Room capabilities are matched to the service requirement | PRD-SVC-002/004 |
| SVC-AT-001 | Optional equipment requirement handled correctly (present/absent) | PRD-SVC-003 |
| RES-AT-004 | FIXED equipment is constrained to its compatible room | PRD-RES-006 |
| RES-AT-005 | Setup/cleanup buffer extends resource consumption window | PRD-SCH-006 |
| AVL-AT-001 | Availability = rules ∩ ¬blocks ∩ ¬reservations | PRD-AVL-003 |
| BKG-AT-004 | Confirmation atomically reserves every required resource or fails | PRD-BKG-004/005/006, PRD-RSV-003 |
| BKG-AT-005 | A booking carries no hardcoded resource ids; assignment change keeps identity | PRD-BKG-001, PRD-ASN-002 |
| BKG-AT-006 | Direct search never reassigns an existing booking | PRD-SCH-004, PRD-ASN-004 |
| BKG-AT-007 | Cancelling a booking releases its reservations; the slot can be rebooked | PRD-BKG-008 |
| BKG-AT-008 | A confirmed booking can be marked COMPLETED or NO_SHOW; invalid transitions rejected | PRD-BKG-009 |
| BKG-AT-009 | A booking can be retrieved by id with its assignments; unknown id → 404 | PRD-BKG-010 |
| BKG-AT-010 | Bookings can be listed for the active tenant, optionally filtered by status | PRD-BKG-011 |
| BKG-AT-011 | A booking can be rescheduled: old slot freed, new slot reserved, identity kept; conflict → 409 | PRD-BKG-012 |
| RSV-AT-001 | A reserved resource is excluded from conflicting options; overlap rejected | PRD-RSV-002/004 |
| RSV-AT-002 | Concurrent confirmations for the same resource → only one succeeds | PRD-RSV-004 |
| SCH-AT-005 | Scheduling services hold no mutable authoritative calendar state | PRD-SCH-001, PRD-NFR-001 |

## 15. Success measures

- **Extensibility:** adding a resource type requires zero scheduling-engine changes
  (validated by RES-AT-001 and ArchUnit rules).
- **Correctness:** zero double-bookings of exclusive resources under concurrency
  (RSV-AT-002); 100% of confirmed bookings fully feasible.
- **Isolation:** zero cross-tenant reads/references in tests (TEN-AT-001/002).
- **Traceability:** every implemented requirement has a traceability entry and passing
  acceptance test; CI documentation validation green.
- **Self-service readiness:** no code path assumes actor==subject==provider or
  all-confirmed/all-public bookings (reviewed per change against ADR-005).

## 16. Design discussions & alternatives considered

- **Generic resource model vs per-type models.** Chosen: generic (ADR-001). Rejected:
  separate staff/room/equipment schedulers — duplicative and closed to new types.
- **Booking with resource FKs vs assignment/reservation separation.** Chosen: separation
  (ADR-003). Rejected: `staffId`/`roomId` on booking — blocks reassignment and self-service.
- **Overlap prevention in app vs database.** Chosen: DB exclusion constraint (ADR-004).
  Rejected: check-then-insert — unsafe under concurrency.
- **Dynamic meta-model/rules engine vs explicit concepts.** Chosen: explicit domain
  concepts with opinionated defaults (ADR-006). Rejected: generic rules platform —
  complexity without near-term value.
- **Build tool.** Chosen: Maven (ADR-007) for ubiquity/predictability for future
  human/AI contributors. Considered: Gradle (better multi-module ergonomics/speed).
- **Migration tool.** Chosen: Flyway plain SQL (ADR-008) for faithful PostgreSQL DDL
  (range types, exclusion constraints, RLS). Considered: Liquibase.
- **Tenancy strategy.** Chosen: shared schema with `tenant_id` + composite FKs + explicit
  scoping (ADR-002). Considered: schema-per-tenant / database-per-tenant (deferred; see
  `06-FUTURE-IDEAS.md`).

## 17. Accepted decisions

See `docs/04-DECISION-REGISTER.md` and `docs/adr/`. All principles in
`PROJECT_CONTEXT.md` §"Accepted design principles" are accepted.

## 18. Open questions

See `docs/07-OPEN-QUESTIONS.md`. None currently block the foundation milestone.

## 19. Deferred features

PRD-SVC-007, PRD-SCH-007, PRD-SCH-008, PRD-BKG-007, PRD-RSV-006, PRD-TEN-008, plus items
in `docs/06-FUTURE-IDEAS.md` (self-service channel, capacity/pooled, notifications,
holds, richer qualification rules, alternative tenancy strategies).
