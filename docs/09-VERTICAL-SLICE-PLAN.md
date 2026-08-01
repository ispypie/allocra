# 09 — Thin Vertical Slice Plan (Deliverable C)

**Status:** IMPLEMENTED — this design has been built and verified end-to-end in CI against
real PostgreSQL (see `docs/08-REQUIREMENTS-TRACEABILITY.md` and the slice test suites). The
two open decisions were resolved as: local stub `TokenVerifier` (DEC-020) and buffers
deferred (DEC-021). Persistence uses `JdbcClient` (DEC-022); slice infra lives in `app`
(DEC-023). The design below stands as the record of what was built.

## 1. Goal

The smallest end-to-end internal-booking path that exercises the load-bearing decisions:
generic resources (ADR-001), booking/assignment/reservation separation (ADR-003),
PostgreSQL reservation integrity (ADR-004), and multi-tenant isolation (ADR-002).

## 2. Scope (the 15 required capabilities)

1. A **tenant**.
2. An **organisation member** (with a Scheduler role → `BOOKING_CREATE` permission).
3. One **location**.
4. **Staff / room / equipment** resource types (`BaseResourceKind` PERSON/PLACE/ASSET).
5. **Resources with capabilities** (staff qualification+expiry; room capability; equipment
   category + fixed-room compatibility).
6. A **service type** (e.g. a treatment) with a nominal duration.
7. One **required staff** requirement.
8. One **required room** requirement.
9. One **optional equipment** requirement.
10. **Direct availability search**.
11. **Booking confirmation**.
12. **Resource assignments** (one per satisfied requirement).
13. **One reservation per consumed exclusive resource**.
14. **Rejection of overlapping reservations** (DB-enforced).
15. **Cross-tenant isolation**.

## 3. Deliberate simplifications (do NOT exceed without an ADR/PRD change)

- Direct availability search only — **no** assignment changes, **no** schedule repair
  (PRD-SCH-007), **no** whole-schedule optimisation (PRD-SCH-008).
- All hard requirements assigned before confirmation (PRD-BKG-005); no partial bookings.
- Requirement **quantity = 1** (OQ-SVC-1); exactly one staff, one room, optional one equipment.
- Search defaults (OQ-SCH-1): **horizon 14 days**, **15-minute increments**, **≤ 50 options**.
- Availability = weekly rule template + blocks (OQ-AVL-1); UTC `timestamptz`/`tstzrange`
  (OQ-TIME-1).
- Role→permission mapping held as a **static domain policy** for the slice (tables deferred);
  authorization is still checked by **permission**, never role name (PRD-MEM-003). *(new
  assumption — see §9; to be recorded in `07-OPEN-QUESTIONS.md` on approval)*
- `BookingSubject` modelled as a **value object embedded on the booking** (neutral fields
  only; not a CRM — PRD-SUB-002).
- No holds (PRD-BKG-007), no capacity/pooled (PRD-RSV-006), no MOVABLE/POOLED behaviour
  (only FIXED equipment→room in the slice; PRD-RES-007 subset).
- Setup/cleanup **buffers**: modelled on equipment and folded into the reserved range;
  included only to satisfy RES-AT-005 (may be de-scoped during review — see §9).

## 4. Data model additions (Flyway migrations)

New migrations after `V1__baseline.sql`. Every tenant-owned table carries `tenant_id`;
composite FKs include `tenant_id` (ADR-002); tenant-scoped uniqueness includes `tenant_id`.

| Migration | Tables |
|-----------|--------|
| `V2__identity_membership.sql` | `application_user` (global: `firebase_uid` unique, email, display_name); `organisation_member` (`tenant_id`,`user_id`, status; unique(`tenant_id`,`user_id`)); `organisation_member_role` (`tenant_id`,`member_id`, role) |
| `V3__resources.sql` | `location`; `resource_type` (base_kind, code; unique(`tenant_id`,code)); `resource` (type_id, location_id nullable, active); `resource_capability` (capability_type, value, level nullable, valid_from/valid_to nullable); `resource_compatibility` (`tenant_id`, resource_id, other_resource_id) |
| `V4__services.sql` | `service_type` (code, duration_minutes; unique(`tenant_id`,code)); `resource_requirement` (service_type_id, base_kind, required bool, selection_mode, required_capability_type nullable, min_level nullable, quantity default 1) |
| `V5__availability.sql` | `availability_rule` (resource_id, day_of_week, start_time, end_time); `blocked_availability` (resource_id, `during tstzrange`) |
| `V6__bookings.sql` | `booking` (service_type_id, subject_* value-object columns, `start_at`,`end_at`, status, channel, created_at); `resource_assignment` (booking_id, requirement_id, resource_id, policy) — composite FKs incl. `tenant_id` |
| `V7__reservations.sql` | `reservation` (booking_id, resource_id, `during tstzrange`, status) + **`EXCLUDE USING gist (tenant_id WITH =, resource_id WITH =, during WITH &&) WHERE (status='ACTIVE')`** (ADR-004) |
| `V8__audit.sql` | `audit_event` (actor, action, target_type, target_id, at, detail jsonb) |

Each migration is exercised by a Testcontainers migration test; the exclusion constraint is
exercised by concurrency tests (Deliverable D).

## 5. Module responsibilities

- `common` — typed IDs (`ResourceId`, `ServiceTypeId`, `RequirementId`, `BookingId`,
  `AssignmentId`, `ReservationId`, `MemberId`, `UserId`, `LocationId`), `TenantId`,
  `TenantContext` (already present), error model.
- `identity` — `ApplicationUser`; Firebase token-verification port (+ a `local`/test stub).
- `membership` — `OrganisationMember`, role/permission policy; permission checks.
- `resources` — `ResourceType`, `Resource`, `ResourceCapability`, `ResourceCompatibility`,
  `Location`.
- `services` — `ServiceType`, `ResourceRequirement`, `RequirementConstraint`.
- `availability` — `AvailabilityRule`, `BlockedAvailability`.
- `scheduling` — **pure** `SchedulingSnapshot` (immutable) + `DirectAvailabilitySearch`
  (candidate generation, hard-constraint filtering, soft-constraint ranking). No I/O.
- `bookings` — `Booking`, `BookingSubject` (VO), `ResourceAssignment`; `ConfirmBooking`
  application service (transactional).
- `reservations` — `Reservation`; reservation repository; conflict → `ReservationConflictException`.
- `audit` — `AuditEvent` writer.
- `app` — web controllers, DTOs, Firebase security filter, tenant resolution, wiring.

Persistence lives in each module's `adapter/persistence`; repositories are **tenant-scoped**
(`findById(TenantId, …)`). JPA entities never leak into `scheduling` (ArchUnit-enforced).

## 6. Direct availability search (pure engine)

**Input** — `SchedulingSnapshot`: the service's requirements; per requirement the candidate
resources with capabilities, availability rules, blocks and existing ACTIVE reservations in
the window; the requested window `[from, to]`; preferences (required/preferred resource ids).

**Algorithm**
1. Generate candidate start times from `from` to `min(to, from + 14d)` in **15-min** steps
   where `start + duration` fits the window.
2. For each start slot, for each **hard** requirement compute feasible resources:
   capability match (qualification type, optional min level, **validity on appointment
   date**), availability (rule covers slot; no block; no reservation overlap incl. buffers),
   location, and REQUIRED-resource enforcement.
3. Resolve a **consistent combination** across requirements (room → compatible equipment →
   staff). Optional equipment: include if a feasible one exists, else the slot is still
   feasible.
4. A slot with a valid combination becomes a ranked `CandidateOption` (assignment set +
   soft score: preferred staff/room bonus, fewer gaps).
5. Return up to **50** ranked options. **Mutates nothing** (PRD-SCH-001).

## 7. Booking confirmation (transactional)

`ConfirmBooking` (application service, `@Transactional`):
1. Authenticate (Firebase token) → `ApplicationUser`; resolve+validate **active tenant** from
   membership (never client-trusted); check `BOOKING_CREATE` permission.
2. Rebuild the snapshot for the chosen slot and **re-validate feasibility** (all hard reqs).
3. Insert `booking` (status `CONFIRMED`, channel `INTERNAL`).
4. Insert one `resource_assignment` per satisfied requirement.
5. Insert one `reservation` per exclusive assigned resource; `during` folds in any buffers.
6. The **exclusion constraint** is the authority: a concurrent conflict → constraint
   violation → rollback → `ReservationConflictException` → **HTTP 409** (PRD-BKG-006).
7. Write an `audit_event`.

## 8. API (internal)

- `POST /v1/services/{serviceTypeId}/availability:search`
  → body `{from,to,subject?,requiredResourceIds?,preferredResourceIds?}` → `200` ranked
  options (bookable **options**, not raw calendars — self-service-ready shape, ADR-005).
- `POST /v1/bookings`
  → body `{serviceTypeId,start,subject,assignments:[{requirementId,resourceId}]}`
  → `201` booking | `409` conflict | `422` infeasible | `403` permission.
- Auth: Firebase ID-token filter resolves the user; the active tenant (header/claim) is
  validated against membership. `local`/test profile uses a stub verifier.

## 9. New assumptions & open decisions (for review)

- **A-SLICE-1** Role→permission mapping as a static domain policy (tables deferred). *(To be
  added to `07-OPEN-QUESTIONS.md` as ASSUMED on approval.)*
- **A-SLICE-2** `BookingSubject` embedded as a VO on `booking` (vs its own table).
- **OQ-BUF-1 (decide)** Include setup/cleanup buffers in the slice (needed for RES-AT-005)
  or defer RES-AT-005 to the next milestone?
- **OQ-AUTH-1 (decide)** Implement the real Firebase filter in the slice, or test the
  application services with a stubbed security context and defer the filter?

## 10. Proposed build order

1. Migrations `V2`–`V8` (+ migration tests).
2. Typed IDs & domain value objects/entities per module.
3. Tenant-scoped persistence adapters + repositories.
4. `scheduling`: snapshot + `DirectAvailabilitySearch` (pure) with **domain tests first**
   (TDD).
5. `bookings`: `ConfirmBooking` transactional use case.
6. `app`: controllers + Firebase filter + tenant resolution.
7. Acceptance + PostgreSQL integration tests (Deliverable D), traceability updates.

Definition of Done (`03-TECHNICAL-SPECIFICATION.md` §10) applies to every step. The
acceptance/test mapping is in `docs/10-ACCEPTANCE-TEST-PLAN.md`.
