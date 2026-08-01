# 10 — Acceptance Test Plan (Deliverable D)

**Status:** PLAN — for review, alongside `docs/09-VERTICAL-SLICE-PLAN.md`. Maps the first
vertical slice to PRD requirements and the acceptance tests that will drive it. Tests are
written **before/with** implementation (TDD; `03-TECHNICAL-SPECIFICATION.md` §8).

**Conventions:** every acceptance test's `@DisplayName` carries both the PRD id and the AT
id (e.g. `PRD-BKG-004 / BKG-AT-004: …`). Test types: **D** = pure domain test, **A** =
acceptance test (application service / web slice), **I** = PostgreSQL integration test
(Testcontainers). Integration tests use real PostgreSQL — never mocked repositories.

## 1. Minimum acceptance criteria (Milestone 1 §20 Deliverable D)

| # | Criterion | AT id | PRD | Type | Planned test class |
|---|-----------|-------|-----|------|--------------------|
| 1 | Tenant isolation — tenant A cannot read/reference tenant B | TEN-AT-001 | PRD-TEN-002/003/006/007 | I | `TenantIsolationIT` |
| 2 | Organisation membership validation — client tenant id not matching membership rejected | TEN-AT-002 | PRD-TEN-004, PRD-SEC-002 | A/I | `ActiveTenantResolutionIT` |
| 2b | Action without required permission rejected | MEM-AT-001 | PRD-MEM-004, PRD-SEC-003 | A | `PermissionAuthorizationTest` |
| 3 | Qualified staff selected | SCH-AT-001 | PRD-SVC-006 | D | `QualificationMatchingTest` |
| 4 | Unqualified/expired staff rejected | SCH-AT-002 | PRD-SVC-006 | D | `QualificationMatchingTest` |
| 5 | Room capabilities matched | RES-AT-002 | PRD-SVC-002/004 | D | `CapabilityMatchingTest` |
| 6 | Optional equipment handled (present & absent) | SVC-AT-001 | PRD-SVC-003 | D | `OptionalRequirementTest` |
| 7 | Fixed equipment constrained to compatible room | RES-AT-004 | PRD-RES-006/008 | D | `EquipmentRoomCompatibilityTest` |
| 8 | Reserved resources excluded from options; overlap rejected | RSV-AT-001 | PRD-RSV-002/004 | D + I | `AvailabilityExclusionTest`, `ReservationOverlapIT` |
| 9 | Atomic booking confirmation (all-or-nothing) | BKG-AT-004 | PRD-BKG-004/005/006, PRD-RSV-003 | I | `ConfirmBookingIT` |
| 10 | Concurrent conflict — only one of N succeeds | RSV-AT-002 | PRD-RSV-004 | I | `ReservationConcurrencyIT` |
| 11 | Booking separated from assignments (no resource ids; reassignment keeps identity) | BKG-AT-005 | PRD-BKG-001, PRD-ASN-002 | D/A | `BookingAssignmentSeparationTest` |
| 12 | Direct search never reassigns an existing booking | BKG-AT-006 | PRD-SCH-004, PRD-ASN-004 | D/A | `DirectSearchNoReassignTest` |

## 2. Additional slice acceptance tests

| AT id | Criterion | PRD | Type | Planned test class |
|-------|-----------|-----|------|--------------------|
| SCH-AT-003 | REQUIRED specific staff enforced as hard constraint | PRD-SVC-005 | D | `StaffSelectionModeTest` |
| SCH-AT-004 | PREFERRED staff ranked higher but not mandatory | PRD-SVC-005 | D | `StaffSelectionModeTest` |
| AVL-AT-001 | Availability = rules ∩ ¬blocks ∩ ¬reservations | PRD-AVL-003 | D | `AvailabilityIntersectionTest` |
| RES-AT-001 | A new resource type is schedulable with no engine change | PRD-RES-003 | D | `GenericResourceTypeTest` |
| RES-AT-005 | Setup/cleanup buffer extends the reserved window | PRD-SCH-006 | D + I | `BufferWindowTest`, `BufferReservationIT` *(gated by OQ-BUF-1)* |
| SCH-AT-005 | Scheduling holds no mutable authoritative calendar state | PRD-SCH-001, PRD-NFR-001 | D | `ArchitectureTest` (existing) + `StatelessEngineTest` |

## 3. Pure domain tests (§16.1 coverage)

`scheduling`/`resources`/`services` domain tests, no Spring, no DB:
capability matching · required-qualification matching · qualification expiry/validity ·
hard vs soft constraints · required vs preferred resources · equipment mobility (FIXED→room)
· resource compatibility · availability intersection · setup/cleanup buffers · assignment
locking (policy shape) · resource exclusivity (search-side).

## 4. PostgreSQL integration tests (§16.3 coverage — Testcontainers)

Real PostgreSQL for: transactions; range types & the exclusion constraint; overlapping
reservations; **concurrent** booking attempts (only one wins); tenant-scoped queries;
cross-tenant reference prevention (composite FK rejects cross-tenant insert); rollback on
failed confirmation; migrations. Key classes: `ConfirmBookingIT`, `ReservationConcurrencyIT`,
`ReservationOverlapIT`, `TenantIsolationIT`, `CrossTenantReferenceIT`, migration tests.

`ReservationConcurrencyIT` sketch — *Given* a resource free for a slot; *When* N threads
confirm bookings for that resource+slot simultaneously; *Then* exactly one returns `201`
and the rest `409`, and exactly one ACTIVE reservation exists.

## 5. Architecture tests (§16.4 coverage)

Existing `ArchitectureTest` (scheduling purity; no module depends on `app`) is extended with:
domain must not depend on Firebase; module dependency direction respected; no
domain-specific scheduler bypasses the generic `Resource` model; infrastructure entities do
not leak into `scheduling`.

## 6. Documentation validation (§16.5 coverage)

Existing `DocumentationValidationTest` already fails CI when: required docs are missing; an
implemented requirement lacks a traceability entry; a traceability entry or a test
references an unknown PRD id; an ADR link is broken; the decision register references a
missing ADR. It will be extended to assert every **AT id** used in the slice appears in this
plan and in `08-REQUIREMENTS-TRACEABILITY.md`.

## 7. Traceability & DoD

Each test above lands in `docs/08-REQUIREMENTS-TRACEABILITY.md` (source location + test
location) **in the same change** as its implementation. A slice feature is done only when
its PRD requirement, acceptance criteria, ADR conformance, domain tests, acceptance tests,
integration tests (where relevant), traceability, tech-spec/PROJECT_CONTEXT updates and CI
are all green (`03-TECHNICAL-SPECIFICATION.md` §10).
