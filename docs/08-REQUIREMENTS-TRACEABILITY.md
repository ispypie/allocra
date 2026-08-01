# 08 — Requirements Traceability

Maps every requirement to its implementation and tests. **This file must be updated in the
same change as any implementation** (Definition of Done #7). CI documentation validation
(PRD-NFR-008) will fail on: an implemented requirement with no entry here; an entry
referencing an unknown requirement id; a test referencing an unknown PRD id.

**Implementation status values:** `PLANNED`, `IN_PROGRESS`, `IMPLEMENTED`, `VERIFIED`,
`DEFERRED`, `SUPERSEDED`.

> **Current state:** Milestone 1 Deliverables A (documentation) and B (engineering
> foundation) complete. Foundation/non-functional requirements now have real
> implementations and tests (see the matrix and the "Deliverable B" summary below).
> Functional (domain) requirements remain `PLANNED`/`DEFERRED` until the vertical slice
> (Deliverable C); their columns are pre-populated with the intended module and planned
> test ids so implementation work slots straight in.

## Traceability matrix

| PRD ID | Summary | Status | Module | Source location | Acceptance test | Test location | Notes |
|--------|---------|--------|--------|-----------------|-----------------|---------------|-------|
| PRD-TEN-001 | Tenant is root of tenant-owned data | IN_PROGRESS | tenancy | `db/migration/V1__baseline.sql` (`tenant` table) | — | app `MigrationIT` | Aggregate/repo in the slice |
| PRD-TEN-002 | `tenant_id` on all tenant-owned data | PLANNED | all | migrations (baseline only so far) | TEN-AT-001 | _tbd_ | Per-table with the slice |
| PRD-TEN-003 | Tenant-scoped repository methods | IN_PROGRESS | common | `TenantId`, `TenantContext` | TEN-AT-001 | common `TenantContextTest` | Repos added with the slice |
| PRD-TEN-004 | Never trust client tenant id | IN_PROGRESS | common/membership | `TenantContext` (resolved, not client-trusted) | TEN-AT-002 | common `TenantContextTest` | Filter/validation in the slice |
| PRD-TEN-005 | Tenant-scoped uniqueness incl. tenant_id | PLANNED | all | _tbd_ (migrations) | — | _tbd_ | |
| PRD-TEN-006 | Composite FKs incl. tenant discriminator | PLANNED | all | _tbd_ (migrations) | TEN-AT-001 | ReservationCrossTenantIntegrationTest (planned) | |
| PRD-TEN-007 | Cross-tenant isolation tested | PLANNED | all | _tbd_ | TEN-AT-001/002 | _tbd_ | |
| PRD-TEN-008 | RLS as optional defence-in-depth | DEFERRED | tenancy | — | — | — | OQ-TEN-1 |
| PRD-IDN-001 | Firebase establishes identity | PLANNED | identity | _tbd_ | — | — | ADR-010 |
| PRD-IDN-002 | PostgreSQL owns app user/membership/roles | PLANNED | identity/membership | _tbd_ | — | — | |
| PRD-IDN-003 | ApplicationUser ≠ person Resource | PLANNED | identity/resources | _tbd_ | — | — | |
| PRD-IDN-004 | Multi-org user; single active tenant | PLANNED | membership | _tbd_ | TEN-AT-002 | _tbd_ | |
| PRD-MEM-001 | OrganisationMember links user↔tenant | PLANNED | membership | _tbd_ | — | — | |
| PRD-MEM-002 | Initial roles | PLANNED | membership | _tbd_ | — | — | |
| PRD-MEM-003 | Permission-based authz (no role-name checks) | PLANNED | membership | _tbd_ | MEM-AT-001 | _tbd_ | |
| PRD-MEM-004 | Membership/permission validated per action | PLANNED | membership | _tbd_ | MEM-AT-001 | _tbd_ | |
| PRD-RES-001 | Generic Resource; no per-type schedulers | IN_PROGRESS | scheduling | `DirectAvailabilitySearch` (operates on `BaseKind`, never concrete types) | RES-AT-001 | scheduling `DirectAvailabilitySearchTest` | Engine domain done; persistence next |
| PRD-RES-002 | Resource has type + BaseResourceKind | PLANNED | resources | _tbd_ | — | — | |
| PRD-RES-003 | New type schedulable, no engine change | IMPLEMENTED | scheduling | `DirectAvailabilitySearch` | RES-AT-001 | scheduling `DirectAvailabilitySearchTest.newResourceTypeIsSchedulable` | Domain-verified |
| PRD-RES-006 | EquipmentMobility incl. FIXED→room (compat) | IN_PROGRESS | scheduling | `ResourceCandidate.compatibleWith` | RES-AT-004 | scheduling `DirectAvailabilitySearchTest.fixedEquipmentConstrainedToCompatibleRoom` | Mobility enum in persistence next |
| PRD-RES-008 | ResourceCompatibility | IMPLEMENTED | scheduling | `ResourceCandidate.compatibleWith` | RES-AT-004 | scheduling `DirectAvailabilitySearchTest.fixedEquipmentConstrainedToCompatibleRoom` | Domain-verified |
| PRD-RES-004 | ResourceCapability | IN_PROGRESS | scheduling/resources | `CapabilitySpec` (engine); persistence pending | SCH-AT-001 | scheduling `DirectAvailabilitySearchTest` | Persisted capability next |
| PRD-RES-005 | Profiles via composition | PLANNED | resources | _tbd_ | — | ArchUnit (planned) | No deep inheritance |
| PRD-RES-006 | EquipmentMobility incl. FIXED→room | PLANNED | resources | _tbd_ | RES-AT-004 | _tbd_ | |
| PRD-RES-007 | Movable/pooled model (subset in slice) | DEFERRED | resources | — | — | — | OQ-BUF-1 |
| PRD-RES-008 | ResourceCompatibility | PLANNED | resources | _tbd_ | RES-AT-004 | _tbd_ | |
| PRD-RES-009 | AttributeDefinition (bounded) | PLANNED | resources | _tbd_ | — | — | |
| PRD-SVC-001 | ServiceType defines requirements | PLANNED | services | _tbd_ | — | — | |
| PRD-SVC-002 | Requirement describes what, not who | IMPLEMENTED | scheduling | `RequirementSpec`, `DirectAvailabilitySearch` | RES-AT-002 | scheduling `DirectAvailabilitySearchTest.roomCapabilitiesAreMatched` | Domain-verified |
| PRD-SVC-003 | Required vs optional requirements | IMPLEMENTED | scheduling | `RequirementSpec.required`, engine skip logic | SVC-AT-001 | scheduling `DirectAvailabilitySearchTest.optionalEquipmentIsHandled` | Domain-verified |
| PRD-SVC-004 | Hard/soft constraints; hard by default | IMPLEMENTED | scheduling | `DirectAvailabilitySearch` (hard filter + soft score) | RES-AT-002 | scheduling `DirectAvailabilitySearchTest` | Domain-verified |
| PRD-SVC-005 | Staff selection REQUIRED/PREFERRED/ANY | IMPLEMENTED | scheduling | `SelectionMode`, `RequirementSpec.allows/prefers` | SCH-AT-003/004 | scheduling `DirectAvailabilitySearchTest.requiredStaffIsEnforced/preferredStaffIsRankedHigher` | Domain-verified |
| PRD-SVC-006 | Qualification matching + expiry/validity | IMPLEMENTED | scheduling | `CapabilityRequirement`, `CapabilitySpec.validOn` | SCH-AT-001/002 | scheduling `DirectAvailabilitySearchTest` (qualified/unqualified/expired/minLevel) | Domain-verified |
| PRD-SVC-007 | Supervision/qualification alternatives | DEFERRED | services | — | — | — | Future |
| PRD-AVL-001 | AvailabilityRule | PLANNED | availability | _tbd_ | AVL-AT-001 | _tbd_ | OQ-AVL-1 |
| PRD-AVL-002 | BlockedAvailability | PLANNED | availability | _tbd_ | AVL-AT-001 | _tbd_ | |
| PRD-AVL-003 | Availability = rules ∩ ¬blocks ∩ ¬reservations | IMPLEMENTED | scheduling | `ResourceCandidate.availableFor` | AVL-AT-001 | scheduling `DirectAvailabilitySearchTest.availabilityIsIntersectionOfRulesBlocksAndReservations` | Domain-verified |
| PRD-SCH-001 | Stateless engine; immutable snapshot | IMPLEMENTED | scheduling | `DirectAvailabilitySearch` (no fields), `SchedulingSnapshot` (immutable) | SCH-AT-005 | scheduling `DirectAvailabilitySearchTest.engineIsStateless`; app `ArchitectureTest` | Domain-verified |
| PRD-SCH-002 | Hard constraints enumerated | IMPLEMENTED | scheduling | `DirectAvailabilitySearch.isFeasible` | SCH-AT-001/002 | scheduling `DirectAvailabilitySearchTest` | Domain-verified |
| PRD-SCH-003 | Soft constraints rank | IMPLEMENTED | scheduling | `DirectAvailabilitySearch` (preferred bonus + sort) | SCH-AT-004 | scheduling `DirectAvailabilitySearchTest.preferredStaffIsRankedHigher` | Domain-verified |
| PRD-SCH-004 | Direct search never reassigns | IMPLEMENTED | scheduling | `DirectAvailabilitySearch` (reservations block, never freed) | BKG-AT-006 | scheduling `DirectAvailabilitySearchTest.directSearchNeverReassignsExistingReservation` | Domain-verified |
| PRD-SCH-005 | Simple search: horizon/increment/limit | PLANNED | scheduling | _tbd_ | — | — | OQ-SCH-1 |
| PRD-SCH-006 | Buffers extend consumption window | PLANNED | scheduling/reservations | _tbd_ | RES-AT-005 | _tbd_ | OQ-BUF-1 |
| PRD-SCH-007 | Schedule repair | DEFERRED | scheduling | — | — | — | DEC-014 |
| PRD-SCH-008 | Whole-schedule optimisation | DEFERRED | scheduling | — | — | — | DEC-015 |
| PRD-SUB-001 | BookingSubject (not Customer) | PLANNED | bookings | _tbd_ | — | — | |
| PRD-SUB-002 | Subject fields; not a CRM | PLANNED | bookings | _tbd_ | — | — | |
| PRD-SUB-003 | Subject ≠ actor/member/staff | PLANNED | bookings | _tbd_ | BKG-AT-005 | _tbd_ | |
| PRD-BKG-001 | Booking has no resource ids | PLANNED | bookings | _tbd_ | BKG-AT-005 | ArchUnit (planned) | ADR-003 |
| PRD-BKG-002 | Booking status lifecycle | PLANNED | bookings | _tbd_ | — | — | |
| PRD-BKG-003 | Booking channel (INTERNAL only now) | PLANNED | bookings | _tbd_ | — | — | |
| PRD-BKG-004 | Atomic confirmation or fail | PLANNED | bookings/reservations | _tbd_ | BKG-AT-004 | ReservationConcurrencyIntegrationTest (planned) | ADR-004 |
| PRD-BKG-005 | Confirm only if fully feasible | PLANNED | bookings | _tbd_ | BKG-AT-004 | _tbd_ | |
| PRD-BKG-006 | Revalidate in txn; 409 on conflict | PLANNED | bookings/reservations | _tbd_ | BKG-AT-004, RSV-AT-002 | _tbd_ | |
| PRD-BKG-007 | Booking holds | DEFERRED | bookings | — | — | — | Future |
| PRD-ASN-001 | ResourceAssignment binds resource→requirement | PLANNED | bookings/reservations | _tbd_ | BKG-AT-004 | _tbd_ | |
| PRD-ASN-002 | Assignment change keeps booking identity | PLANNED | bookings | _tbd_ | BKG-AT-005 | _tbd_ | |
| PRD-ASN-003 | AssignmentPolicy (behaviour deferred) | DEFERRED | reservations | — | — | — | |
| PRD-ASN-004 | Search never silently reassigns | IMPLEMENTED | scheduling | `DirectAvailabilitySearch` (read-only; reservations block) | BKG-AT-006 | scheduling `DirectAvailabilitySearchTest.directSearchNeverReassignsExistingReservation` | Domain-verified |
| PRD-RSV-001 | Reservation prevents conflicting use | PLANNED | reservations | _tbd_ | RSV-AT-001 | _tbd_ | |
| PRD-RSV-002 | Resources exclusive by default | PLANNED | reservations | _tbd_ | RSV-AT-001 | _tbd_ | |
| PRD-RSV-003 | One reservation per exclusive resource | PLANNED | reservations | _tbd_ | BKG-AT-004 | _tbd_ | |
| PRD-RSV-004 | DB overlap prevention; one winner | PLANNED | reservations | _tbd_ (exclusion constraint) | RSV-AT-001/002 | ReservationConcurrencyIntegrationTest (planned) | ADR-004 |
| PRD-RSV-005 | Search advisory; conflict at confirm | PLANNED | bookings | _tbd_ | RSV-AT-002 | _tbd_ | |
| PRD-RSV-006 | Capacity/pooled model | DEFERRED | reservations | — | — | — | Future |
| PRD-AUD-001 | AuditEvent for key actions | PLANNED | audit | _tbd_ | — | — | |
| PRD-NFR-001 | Stateless, horizontally scalable | IN_PROGRESS | app/scheduling/common | common `TenantContext` (per-request only); scheduling `SchedulingSnapshot` | SCH-AT-005 | ArchitectureTest | Full validation with the slice |
| PRD-NFR-002 | Cloud Run deployable | IMPLEMENTED | app | `Dockerfile`, `application.yml` (`PORT`, graceful shutdown), `application-cloud.yml` | — | ApplicationSmokeIT | |
| PRD-NFR-003 | Health/liveness/readiness | VERIFIED | app | `application.yml` (actuator probes) | — | app `ApplicationSmokeIT` | Probes assert UP against Testcontainers |
| PRD-NFR-004 | Structured logging | IMPLEMENTED | app | `application-cloud.yml` (`logging.structured.format.console=ecs`) | — | — | Config-only; no automated assertion |
| PRD-NFR-005 | Flyway migrations, tested | VERIFIED | app | `db/migration/V1__baseline.sql` | — | app `MigrationIT` | btree_gist + tenant table asserted |
| PRD-NFR-006 | Testcontainers for DB tests | VERIFIED | app (test) | `MigrationIT`, `ApplicationSmokeIT` | — | those tests | |
| PRD-NFR-007 | ArchUnit boundary rules | VERIFIED | app (test) | `ArchitectureTest` (4 rules) | — | app `ArchitectureTest` | scheduling purity + app-dep rule |
| PRD-NFR-008 | CI incl. doc validation | VERIFIED | ci / app (test) | `.github/workflows/ci.yml`, `DocumentationValidationTest` | — | app `DocumentationValidationTest` | |
| PRD-NFR-009 | Java 21 + Spring Boot 3.x | IMPLEMENTED | (build) | root `pom.xml` (`release 21`, Spring Boot 3.4.1) | — | — | |
| PRD-SEC-001 | Authenticated requests | PLANNED | identity | _tbd_ | — | — | |
| PRD-SEC-002 | Active tenant from membership | PLANNED | membership | _tbd_ | TEN-AT-002 | _tbd_ | |
| PRD-SEC-003 | Permission-based authz, auditable denials | PLANNED | membership/audit | _tbd_ | MEM-AT-001 | _tbd_ | |
| PRD-SEC-004 | No secrets/PII in logs | PLANNED | app | _tbd_ | — | — | |

## Example entry format (for future implemented requirements)

```
Requirement: PRD-BKG-004
Summary: Booking confirmation atomically reserves every required resource or fails.
Implementation:
  - bookings application service (modules/bookings/application)
  - reservation repository (modules/reservations/adapter/persistence)
  - PostgreSQL transaction boundary + exclusion constraint (db migration V__)
Tests:
  - BKG-AT-004 (acceptance)
  - ReservationConcurrencyIntegrationTest (Testcontainers)
Status: IMPLEMENTED
Notes: Conflict surfaced as 409; buffers folded into reserved range.
```
