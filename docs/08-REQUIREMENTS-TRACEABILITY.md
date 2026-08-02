# 08 — Requirements Traceability

Maps every requirement to its implementation and tests. **This file must be updated in the
same change as any implementation** (Definition of Done #7). CI documentation validation
(PRD-NFR-008) will fail on: an implemented requirement with no entry here; an entry
referencing an unknown requirement id; a test referencing an unknown PRD id.

**Implementation status values:** `PLANNED`, `IN_PROGRESS`, `IMPLEMENTED`, `VERIFIED`,
`DEFERRED`, `SUPERSEDED`.

> **Current state:** Milestone 1 Deliverables A–D complete; the thin internal-booking
> vertical slice is IMPLEMENTED and green in CI. Functional requirements exercised by the
> slice are `VERIFIED` (domain via `DirectAvailabilitySearchTest`; DB/flow via
> `BookingSliceIT`, `ReservationSchemaIT`, `MigrationIT` on Testcontainers). Requirements
> not exercised by the slice remain `PLANNED`; `RES-AT-005` (buffers) is `DEFERRED`
> (DEC-021). Persistence uses `JdbcClient` in the `app` module (DEC-022/023).
>
> **Slice test suites:** `com.allocra.scheduling.DirectAvailabilitySearchTest` (14 domain
> tests); `com.allocra.app.it.BookingSliceIT` (confirm/concurrency/tenant/permission/auth);
> `com.allocra.app.it.ReservationSchemaIT` (exclusion constraint);
> `com.allocra.app.it.MigrationIT` + `ApplicationSmokeIT` (schema/boot).

## Traceability matrix

| PRD ID | Summary | Status | Module | Source location | Acceptance test | Test location | Notes |
|--------|---------|--------|--------|-----------------|-----------------|---------------|-------|
| PRD-TEN-001 | Tenant is root of tenant-owned data | IN_PROGRESS | tenancy | `db/migration/V1__baseline.sql` (`tenant` table) | — | app `MigrationIT` | Aggregate/repo in the slice |
| PRD-TEN-002 | `tenant_id` on all tenant-owned data | VERIFIED | all | migrations V2–V8 (every tenant-owned table) | TEN-AT-001 | app `BookingSliceIT` | |
| PRD-TEN-003 | Tenant-scoped repository methods | IN_PROGRESS | common | `TenantId`, `TenantContext` | TEN-AT-001 | common `TenantContextTest` | Repos added with the slice |
| PRD-TEN-004 | Never trust client tenant id | VERIFIED | app | `TenantAuthFilter` (membership-validated), `AuthRepository` | TEN-AT-002 | app `BookingSliceIT.crossTenantRejected` | |
| PRD-TEN-005 | Tenant-scoped uniqueness incl. tenant_id | IMPLEMENTED | all | migrations V2–V8 (unique incl. tenant_id) | — | app `MigrationIT` | |
| PRD-TEN-006 | Composite FKs incl. tenant discriminator | VERIFIED | all | migrations V2–V8 composite FKs | TEN-AT-001 | app `ReservationSchemaIT` | |
| PRD-TEN-007 | Cross-tenant isolation tested | VERIFIED | app | tenant-scoped repos + membership filter | TEN-AT-001/002 | app `BookingSliceIT` | |
| PRD-TEN-008 | RLS as optional defence-in-depth | DEFERRED | tenancy | — | — | — | OQ-TEN-1 |
| PRD-IDN-001 | Firebase establishes identity | IMPLEMENTED | identity/app | `TokenVerifier` port + `StubTokenVerifier` (real Firebase deferred, DEC-020) | — | app `BookingSliceIT` | ADR-010 |
| PRD-IDN-002 | PostgreSQL owns app user/membership/roles | VERIFIED | identity/membership/app | `application_user`/`organisation_member`/roles; `AuthRepository` | — | app `BookingSliceIT` | |
| PRD-IDN-003 | ApplicationUser ≠ person Resource | PLANNED | identity/resources | _tbd_ | — | — | |
| PRD-IDN-004 | Multi-org user; single active tenant | VERIFIED | membership/app | active tenant from `X-Tenant-Id` validated per request | TEN-AT-002 | app `BookingSliceIT.crossTenantRejected` | |
| PRD-MEM-001 | OrganisationMember links user↔tenant | VERIFIED | membership/app | `Membership`, `organisation_member`, `AuthRepository` | — | app `BookingSliceIT` | |
| PRD-MEM-002 | Initial roles | PLANNED | membership | _tbd_ | — | — | |
| PRD-MEM-003 | Permission-based authz (no role-name checks) | VERIFIED | membership/app | `Permission`, `Role.permissions()`, `Membership.has()` | MEM-AT-001 | app `BookingSliceIT.viewerCannotConfirm` | |
| PRD-MEM-004 | Membership/permission validated per action | VERIFIED | app | `ConfirmBookingService` permission check | MEM-AT-001 | app `BookingSliceIT.viewerCannotConfirm` | |
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
| PRD-RES-010 | Create locations/types/resources via API | VERIFIED | app | `ConfigController`, `ConfigService`, `ConfigWriteRepository` | RES-AT-006 | app `ConfigAndBookIT.configureThenBook` | RESOURCE_MANAGE |
| PRD-SVC-001 | ServiceType defines requirements | VERIFIED | services/app | `service_type`/`resource_requirement`, `CatalogRepository` | — | app `BookingSliceIT` | |
| PRD-SVC-002 | Requirement describes what, not who | IMPLEMENTED | scheduling | `RequirementSpec`, `DirectAvailabilitySearch` | RES-AT-002 | scheduling `DirectAvailabilitySearchTest.roomCapabilitiesAreMatched` | Domain-verified |
| PRD-SVC-003 | Required vs optional requirements | IMPLEMENTED | scheduling | `RequirementSpec.required`, engine skip logic | SVC-AT-001 | scheduling `DirectAvailabilitySearchTest.optionalEquipmentIsHandled` | Domain-verified |
| PRD-SVC-004 | Hard/soft constraints; hard by default | IMPLEMENTED | scheduling | `DirectAvailabilitySearch` (hard filter + soft score) | RES-AT-002 | scheduling `DirectAvailabilitySearchTest` | Domain-verified |
| PRD-SVC-005 | Staff selection REQUIRED/PREFERRED/ANY | IMPLEMENTED | scheduling | `SelectionMode`, `RequirementSpec.allows/prefers` | SCH-AT-003/004 | scheduling `DirectAvailabilitySearchTest.requiredStaffIsEnforced/preferredStaffIsRankedHigher` | Domain-verified |
| PRD-SVC-006 | Qualification matching + expiry/validity | IMPLEMENTED | scheduling | `CapabilityRequirement`, `CapabilitySpec.validOn` | SCH-AT-001/002 | scheduling `DirectAvailabilitySearchTest` (qualified/unqualified/expired/minLevel) | Domain-verified |
| PRD-SVC-007 | Supervision/qualification alternatives | DEFERRED | services | — | — | — | Future |
| PRD-SVC-008 | Create services (+requirements) via API | VERIFIED | app | `ConfigController.createService`, `ConfigService` | SVC-AT-002 | app `ConfigAndBookIT.configureThenBook` | SERVICE_MANAGE |
| PRD-AVL-001 | AvailabilityRule | VERIFIED | availability/app | `availability_rule`; expanded by `CandidateRepository` | AVL-AT-001 | scheduling `DirectAvailabilitySearchTest`; app `BookingSliceIT` | OQ-AVL-1 |
| PRD-AVL-002 | BlockedAvailability | IMPLEMENTED | availability/app | `blocked_availability`; applied by `CandidateRepository` | AVL-AT-001 | scheduling `DirectAvailabilitySearchTest` | |
| PRD-AVL-004 | Create availability rules via API | VERIFIED | app | `ConfigController.createAvailabilityRule`, `ConfigService` | AVL-AT-002 | app `ConfigAndBookIT.configureThenBook` | AVAILABILITY_MANAGE |
| PRD-AVL-005 | Location operating hours | VERIFIED | app | `V9` schema; `CandidateRepository` effective-availability; `ConfigController` operating-hours endpoint | AVL-AT-003 | app `BusinessHoursIT` | |
| PRD-AVL-006 | Location closures (full-day) | VERIFIED | app | `V9` schema; `CandidateRepository`; `ConfigController` closures endpoint | AVL-AT-004 | app `BusinessHoursIT` | |
| PRD-AVL-007 | Location timezone (DST-aware) | VERIFIED | app | `location.timezone`; `CandidateRepository` zone conversion (Europe/London BST verified) | AVL-AT-003 | app `BusinessHoursIT` | |
| PRD-AVL-008 | Per-person working hours/days | VERIFIED | availability/scheduling | `availability_rule` + `blocked_availability` (staff = PERSON resource) | AVL-AT-001 | scheduling `DirectAvailabilitySearchTest`; app `BookingSliceIT` | Already implemented |
| PRD-SVC-009 | Service lead/prep time | PLANNED | services | _tbd_ (buffer model) | — | — | Recorded; not built |
| PRD-RES-012 | Resource setup/cleanup time | PLANNED | resources | _tbd_ (buffer model) | RES-AT-005 | _tbd_ | Recorded; not built |
| PRD-AVL-003 | Availability = rules ∩ ¬blocks ∩ ¬reservations | IMPLEMENTED | scheduling | `ResourceCandidate.availableFor` | AVL-AT-001 | scheduling `DirectAvailabilitySearchTest.availabilityIsIntersectionOfRulesBlocksAndReservations` | Domain-verified |
| PRD-SCH-001 | Stateless engine; immutable snapshot | IMPLEMENTED | scheduling | `DirectAvailabilitySearch` (no fields), `SchedulingSnapshot` (immutable) | SCH-AT-005 | scheduling `DirectAvailabilitySearchTest.engineIsStateless`; app `ArchitectureTest` | Domain-verified |
| PRD-SCH-002 | Hard constraints enumerated | IMPLEMENTED | scheduling | `DirectAvailabilitySearch.isFeasible` | SCH-AT-001/002 | scheduling `DirectAvailabilitySearchTest` | Domain-verified |
| PRD-SCH-003 | Soft constraints rank | IMPLEMENTED | scheduling | `DirectAvailabilitySearch` (preferred bonus + sort) | SCH-AT-004 | scheduling `DirectAvailabilitySearchTest.preferredStaffIsRankedHigher` | Domain-verified |
| PRD-SCH-004 | Direct search never reassigns | IMPLEMENTED | scheduling | `DirectAvailabilitySearch` (reservations block, never freed) | BKG-AT-006 | scheduling `DirectAvailabilitySearchTest.directSearchNeverReassignsExistingReservation` | Domain-verified |
| PRD-SCH-005 | Simple search: horizon/increment/limit | PLANNED | scheduling | _tbd_ | — | — | OQ-SCH-1 |
| PRD-SCH-006 | Buffers extend consumption window | PLANNED | scheduling/reservations | _tbd_ | RES-AT-005 | _tbd_ | OQ-BUF-1 |
| PRD-SCH-007 | Schedule repair | DEFERRED | scheduling | — | — | — | DEC-014 |
| PRD-SCH-008 | Whole-schedule optimisation | DEFERRED | scheduling | — | — | — | DEC-015 |
| PRD-SUB-001 | BookingSubject (not Customer) | VERIFIED | bookings/app | `BookingSubject` VO; embedded on `booking` | — | app `BookingSliceIT` | |
| PRD-SUB-002 | Subject fields; not a CRM | PLANNED | bookings | _tbd_ | — | — | |
| PRD-SUB-003 | Subject ≠ actor/member/staff | PLANNED | bookings | _tbd_ | BKG-AT-005 | _tbd_ | |
| PRD-BKG-001 | Booking has no resource ids | VERIFIED | bookings | `Booking` (no resource fields), `resource_assignment` table | BKG-AT-005 | app `BookingSliceIT` | ADR-003 |
| PRD-BKG-002 | Booking status lifecycle | IMPLEMENTED | bookings | `BookingStatus`, `booking.status` | — | app `BookingSliceIT` | Cancel/complete endpoints later |
| PRD-BKG-003 | Booking channel (INTERNAL only now) | IMPLEMENTED | bookings | `BookingChannel`; confirm sets INTERNAL | — | app `BookingSliceIT` | |
| PRD-BKG-004 | Atomic confirmation or fail | VERIFIED | app/bookings/reservations | `ConfirmBookingService` (@Transactional), `BookingWriteRepository` | BKG-AT-004 | app `BookingSliceIT.searchThenConfirmThenSlotTaken` | ADR-004 |
| PRD-BKG-005 | Confirm only if fully feasible | VERIFIED | app | `ConfirmBookingService` (hard-req + engine re-check) | BKG-AT-004 | app `BookingSliceIT` | |
| PRD-BKG-006 | Revalidate in txn; 409 on conflict | VERIFIED | app/reservations | `ConfirmBookingService`, `GlobalExceptionHandler` (409) | BKG-AT-004, RSV-AT-002 | app `BookingSliceIT.concurrentConfirmsOnlyOneSucceeds` | |
| PRD-BKG-007 | Booking holds | DEFERRED | bookings | — | — | — | Future |
| PRD-BKG-008 | Cancel releases reservations; slot rebookable | VERIFIED | app | `BookingLifecycleService.cancel`, `BookingWriteRepository.releaseReservations` | BKG-AT-007 | app `BookingSliceIT.cancelReleasesReservationsAndFreesSlot` | BOOKING_CANCEL |
| PRD-BKG-009 | COMPLETED / NO_SHOW transitions | VERIFIED | app | `BookingLifecycleService.complete/noShow` (guarded from CONFIRMED) | BKG-AT-008 | app `BookingSliceIT.completeAndNoShowAndInvalidTransition` | BOOKING_UPDATE |
| PRD-BKG-010 | Retrieve booking by id with assignments | VERIFIED | app | `BookingQueryService.get`, `BookingReadRepository.findById` | BKG-AT-009 | app `BookingSliceIT.getBookingReturnsAssignments` | BOOKING_VIEW |
| PRD-BKG-011 | List bookings for active tenant (status filter) | VERIFIED | app | `BookingQueryService.list`, `BookingReadRepository.list` | BKG-AT-010 | app `BookingSliceIT.listBookings` | BOOKING_VIEW |
| PRD-BKG-012 | Reschedule keeps identity; atomic release+reserve | VERIFIED | app | `RescheduleBookingService` (@Transactional), `BookingWriteRepository.updateSlot` | BKG-AT-011 | app `BookingSliceIT.rescheduleMovesBookingAndFreesOldSlot` | BOOKING_RESCHEDULE |
| PRD-ASN-001 | ResourceAssignment binds resource→requirement | VERIFIED | bookings/app | `ResourceAssignment`, `resource_assignment` table | BKG-AT-004 | app `BookingSliceIT` | |
| PRD-ASN-002 | Assignment change keeps booking identity | IMPLEMENTED | bookings | assignments are separate rows from `booking` (ADR-003) | BKG-AT-005 | app `BookingSliceIT` | Reassignment endpoint later |
| PRD-ASN-003 | AssignmentPolicy (behaviour deferred) | DEFERRED | reservations | — | — | — | |
| PRD-ASN-004 | Search never silently reassigns | IMPLEMENTED | scheduling | `DirectAvailabilitySearch` (read-only; reservations block) | BKG-AT-006 | scheduling `DirectAvailabilitySearchTest.directSearchNeverReassignsExistingReservation` | Domain-verified |
| PRD-RSV-001 | Reservation prevents conflicting use | VERIFIED | reservations | `reservation` table + exclusion constraint | RSV-AT-001 | app `ReservationSchemaIT` | |
| PRD-RSV-002 | Resources exclusive by default | VERIFIED | app/reservations | one reservation per assigned resource; exclusion constraint | RSV-AT-001 | app `ReservationSchemaIT`, `BookingSliceIT` | |
| PRD-RSV-003 | One reservation per exclusive resource | VERIFIED | app | `ConfirmBookingService` (reservation per assignment) | BKG-AT-004 | app `BookingSliceIT` | |
| PRD-RSV-004 | DB overlap prevention; one winner | VERIFIED | db/reservations | `V7` exclusion constraint; conflict→409 | RSV-AT-001/002 | app `ReservationSchemaIT`, `BookingSliceIT.concurrentConfirmsOnlyOneSucceeds` | ADR-004 |
| PRD-RSV-005 | Search advisory; conflict at confirm | VERIFIED | app | search read-only; confirm re-validates | RSV-AT-002 | app `BookingSliceIT` | |
| PRD-RSV-006 | Capacity/pooled model | DEFERRED | reservations | — | — | — | Future |
| PRD-AUD-001 | AuditEvent for key actions | IMPLEMENTED | audit/app | `audit_event`; `BookingWriteRepository.insertAudit` on confirm | — | app `BookingSliceIT` | |
| PRD-NFR-001 | Stateless, horizontally scalable | IN_PROGRESS | app/scheduling/common | common `TenantContext` (per-request only); scheduling `SchedulingSnapshot` | SCH-AT-005 | ArchitectureTest | Full validation with the slice |
| PRD-NFR-002 | Cloud Run deployable | IMPLEMENTED | app | `Dockerfile`, `application.yml` (`PORT`, graceful shutdown), `application-cloud.yml` | — | ApplicationSmokeIT | |
| PRD-NFR-003 | Health/liveness/readiness | VERIFIED | app | `application.yml` (actuator probes) | — | app `ApplicationSmokeIT` | Probes assert UP against Testcontainers |
| PRD-NFR-004 | Structured logging | IMPLEMENTED | app | `application-cloud.yml` (`logging.structured.format.console=ecs`) | — | — | Config-only; no automated assertion |
| PRD-NFR-005 | Flyway migrations, tested | VERIFIED | app | `db/migration/V1__baseline.sql` | — | app `MigrationIT` | btree_gist + tenant table asserted |
| PRD-NFR-006 | Testcontainers for DB tests | VERIFIED | app (test) | `MigrationIT`, `ApplicationSmokeIT` | — | those tests | |
| PRD-NFR-007 | ArchUnit boundary rules | VERIFIED | app (test) | `ArchitectureTest` (4 rules) | — | app `ArchitectureTest` | scheduling purity + app-dep rule |
| PRD-NFR-008 | CI incl. doc validation | VERIFIED | ci / app (test) | `.github/workflows/ci.yml`, `DocumentationValidationTest` | — | app `DocumentationValidationTest` | |
| PRD-NFR-009 | Java 21 + Spring Boot 3.x | IMPLEMENTED | (build) | root `pom.xml` (`release 21`, Spring Boot 3.4.1) | — | — | |
| PRD-NFR-010 | Client-agnostic API; CORS for multiple UI clients | VERIFIED | app | `CorsConfig` (env-driven, ahead of auth) | NFR-AT-001 | app `CorsIT` | ADR-011 |
| PRD-NFR-011 | Publish OpenAPI spec | VERIFIED | app | springdoc dependency; `/v3/api-docs` + `/swagger-ui` (auth-bypassed) | NFR-AT-002 | app `OpenApiIT` | ADR-011 |
| PRD-SEC-001 | Authenticated requests | VERIFIED | app | `TenantAuthFilter` (bearer required) | — | app `BookingSliceIT.unauthenticatedRejected` | |
| PRD-SEC-002 | Active tenant from membership | VERIFIED | app | `TenantAuthFilter` + `AuthRepository` | TEN-AT-002 | app `BookingSliceIT.crossTenantRejected` | |
| PRD-SEC-003 | Permission-based authz, auditable denials | VERIFIED | app/audit | `ConfirmBookingService` permission check; audit on success | MEM-AT-001 | app `BookingSliceIT.viewerCannotConfirm` | |
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
