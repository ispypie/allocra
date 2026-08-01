# 08 — Requirements Traceability

Maps every requirement to its implementation and tests. **This file must be updated in the
same change as any implementation** (Definition of Done #7). CI documentation validation
(PRD-NFR-008) will fail on: an implemented requirement with no entry here; an entry
referencing an unknown requirement id; a test referencing an unknown PRD id.

**Implementation status values:** `PLANNED`, `IN_PROGRESS`, `IMPLEMENTED`, `VERIFIED`,
`DEFERRED`, `SUPERSEDED`.

> **Current state:** Milestone 1 Deliverable A (documentation) complete. No product code
> exists yet, so all functional requirements are `PLANNED` or `DEFERRED`. The columns are
> pre-populated with the intended module, planned acceptance test id and (where known) the
> planned integration test, so implementation work slots straight in.

## Traceability matrix

| PRD ID | Summary | Status | Module | Source location | Acceptance test | Test location | Notes |
|--------|---------|--------|--------|-----------------|-----------------|---------------|-------|
| PRD-TEN-001 | Tenant is root of tenant-owned data | PLANNED | tenancy | _tbd_ | — | — | Foundational |
| PRD-TEN-002 | `tenant_id` on all tenant-owned data | PLANNED | all | _tbd_ (migrations) | TEN-AT-001 | _tbd_ | Enforced in schema |
| PRD-TEN-003 | Tenant-scoped repository methods | PLANNED | common/all | _tbd_ | TEN-AT-001 | _tbd_ | ArchUnit convention |
| PRD-TEN-004 | Never trust client tenant id | PLANNED | membership/identity | _tbd_ | TEN-AT-002 | _tbd_ | With PRD-SEC-002 |
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
| PRD-RES-001 | Generic Resource; no per-type schedulers | PLANNED | resources/scheduling | _tbd_ | RES-AT-001 | ArchUnit (planned) | ADR-001 |
| PRD-RES-002 | Resource has type + BaseResourceKind | PLANNED | resources | _tbd_ | — | — | |
| PRD-RES-003 | New type schedulable, no engine change | PLANNED | resources/scheduling | _tbd_ | RES-AT-001 | _tbd_ | |
| PRD-RES-004 | ResourceCapability | PLANNED | resources | _tbd_ | SCH-AT-001 | _tbd_ | |
| PRD-RES-005 | Profiles via composition | PLANNED | resources | _tbd_ | — | ArchUnit (planned) | No deep inheritance |
| PRD-RES-006 | EquipmentMobility incl. FIXED→room | PLANNED | resources | _tbd_ | RES-AT-004 | _tbd_ | |
| PRD-RES-007 | Movable/pooled model (subset in slice) | DEFERRED | resources | — | — | — | OQ-BUF-1 |
| PRD-RES-008 | ResourceCompatibility | PLANNED | resources | _tbd_ | RES-AT-004 | _tbd_ | |
| PRD-RES-009 | AttributeDefinition (bounded) | PLANNED | resources | _tbd_ | — | — | |
| PRD-SVC-001 | ServiceType defines requirements | PLANNED | services | _tbd_ | — | — | |
| PRD-SVC-002 | Requirement describes what, not who | PLANNED | services | _tbd_ | RES-AT-002 | _tbd_ | |
| PRD-SVC-003 | Required vs optional requirements | PLANNED | services | _tbd_ | SVC-AT-001 | _tbd_ | |
| PRD-SVC-004 | Hard/soft constraints; hard by default | PLANNED | services/scheduling | _tbd_ | RES-AT-002 | _tbd_ | |
| PRD-SVC-005 | Staff selection REQUIRED/PREFERRED/ANY | PLANNED | services/scheduling | _tbd_ | SCH-AT-003/004 | _tbd_ | |
| PRD-SVC-006 | Qualification matching + expiry/validity | PLANNED | scheduling | _tbd_ | SCH-AT-001/002 | _tbd_ | |
| PRD-SVC-007 | Supervision/qualification alternatives | DEFERRED | services | — | — | — | Future |
| PRD-AVL-001 | AvailabilityRule | PLANNED | availability | _tbd_ | AVL-AT-001 | _tbd_ | OQ-AVL-1 |
| PRD-AVL-002 | BlockedAvailability | PLANNED | availability | _tbd_ | AVL-AT-001 | _tbd_ | |
| PRD-AVL-003 | Availability = rules ∩ ¬blocks ∩ ¬reservations | PLANNED | scheduling | _tbd_ | AVL-AT-001 | _tbd_ | |
| PRD-SCH-001 | Stateless engine; immutable snapshot | PLANNED | scheduling | _tbd_ | SCH-AT-005 | ArchUnit (planned) | ADR-001 |
| PRD-SCH-002 | Hard constraints enumerated | PLANNED | scheduling | _tbd_ | SCH-AT-001/002 | _tbd_ | |
| PRD-SCH-003 | Soft constraints rank | PLANNED | scheduling | _tbd_ | SCH-AT-004 | _tbd_ | |
| PRD-SCH-004 | Direct search never reassigns | PLANNED | scheduling | _tbd_ | BKG-AT-006 | _tbd_ | |
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
| PRD-ASN-004 | Search never silently reassigns | PLANNED | scheduling | _tbd_ | BKG-AT-006 | _tbd_ | |
| PRD-RSV-001 | Reservation prevents conflicting use | PLANNED | reservations | _tbd_ | RSV-AT-001 | _tbd_ | |
| PRD-RSV-002 | Resources exclusive by default | PLANNED | reservations | _tbd_ | RSV-AT-001 | _tbd_ | |
| PRD-RSV-003 | One reservation per exclusive resource | PLANNED | reservations | _tbd_ | BKG-AT-004 | _tbd_ | |
| PRD-RSV-004 | DB overlap prevention; one winner | PLANNED | reservations | _tbd_ (exclusion constraint) | RSV-AT-001/002 | ReservationConcurrencyIntegrationTest (planned) | ADR-004 |
| PRD-RSV-005 | Search advisory; conflict at confirm | PLANNED | bookings | _tbd_ | RSV-AT-002 | _tbd_ | |
| PRD-RSV-006 | Capacity/pooled model | DEFERRED | reservations | — | — | — | Future |
| PRD-AUD-001 | AuditEvent for key actions | PLANNED | audit | _tbd_ | — | — | |
| PRD-NFR-001 | Stateless, horizontally scalable | PLANNED | app/scheduling | _tbd_ | SCH-AT-005 | _tbd_ | |
| PRD-NFR-002 | Cloud Run deployable | PLANNED | app | _tbd_ | — | — | Deliverable B |
| PRD-NFR-003 | Health/liveness/readiness | PLANNED | app | _tbd_ | — | — | Deliverable B |
| PRD-NFR-004 | Structured logging | PLANNED | app | _tbd_ | — | — | Deliverable B |
| PRD-NFR-005 | Flyway migrations, tested | PLANNED | app/db | _tbd_ | — | MigrationIntegrationTest (planned) | Deliverable B |
| PRD-NFR-006 | Testcontainers for DB tests | PLANNED | (test) | _tbd_ | — | — | Deliverable B |
| PRD-NFR-007 | ArchUnit boundary rules | PLANNED | (test) | _tbd_ | — | ArchitectureTest (planned) | Deliverable B |
| PRD-NFR-008 | CI incl. doc validation | PLANNED | ci | _tbd_ | — | DocumentationValidationTest (planned) | Deliverable B |
| PRD-NFR-009 | Java 21 + Spring Boot 3.x | PLANNED | app | _tbd_ | — | — | ADR-009 |
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
