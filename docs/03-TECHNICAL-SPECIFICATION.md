# 03 — Technical Specification

Authoritative for architecture, data model, contracts and engineering standards. When
this conflicts with an accepted ADR, the ADR wins (see `PROJECT_CONTEXT.md` precedence).

## 1. Technology stack

| Area | Choice | Notes / ADR |
|------|--------|-------------|
| Language | Java 21 (LTS) | ADR-009 |
| Framework | Spring Boot 3.x | ADR-009 |
| Build | Maven (multi-module reactor) | ADR-007 |
| Datastore | PostgreSQL (authoritative) | ADR-002, ADR-004 |
| Migrations | Flyway (plain SQL) | ADR-008 |
| Identity | Firebase Authentication (ID token verification) | ADR-010 |
| Auth (server) | Spring Security resource-server style filter | ADR-010 |
| Persistence | Spring Data JPA / JDBC (repositories tenant-scoped) | ADR-002 |
| Integration tests | Testcontainers (PostgreSQL) | ADR-004, PRD-NFR-006 |
| Unit/acceptance tests | JUnit 5 | PRD-NFR |
| Architecture tests | ArchUnit | PRD-NFR-007 |
| Logging | Structured JSON (SLF4J + Logback JSON encoder) | PRD-NFR-004 |
| Deployment | Google Cloud Run (container) | PRD-NFR-002 |
| Formatting / static analysis | Spotless + (Checkstyle/PMD/SpotBugs or equivalent) | Deliverable B |

## 2. Architecture

### 2.1 Style
Modular monolith, single deployable, stateless, horizontally scalable on Cloud Run. Each
module is internally layered:

```
module/
  domain/        pure Java: entities, value objects, domain services, ports (interfaces)
  application/   use-case services, transaction boundaries, orchestration
  adapter/
    persistence/ JPA entities, repository implementations, mappers
    web/         controllers, DTOs (only in modules that expose HTTP)
```

### 2.2 Maven module layout (reactor)
```
allocra (pom, packaging=pom)
├── app                (Spring Boot application; depends on all modules; wiring only)
├── common             (typed IDs, TenantContext, value types, error model)
├── identity
├── tenancy
├── membership
├── resources
├── services
├── availability
├── scheduling         (PURE domain; minimal deps; no Spring web / JPA / Firebase)
├── bookings
├── reservations
└── audit
```
Migrations live under `app` (or a dedicated `db` module) as Flyway SQL on the classpath.

### 2.3 Dependency rules (ArchUnit-enforced, PRD-NFR-007)
- `scheduling` must not depend on: Spring MVC/Web, HTTP types, Firebase SDK, JPA/Hibernate
  entities, Cloud Run/GCP specifics.
- No module's `domain` may import another module's `adapter`.
- Allowed dependency direction follows `PROJECT_CONTEXT`/domain-model module map; `app`
  depends on all, nothing depends on `app`.
- Domain-specific schedulers must not bypass the generic `Resource` model (no per-type
  scheduling classes that read staff/room/equipment directly).
- Infrastructure entities must not leak into `scheduling` (it consumes snapshots/domain
  inputs only).

### 2.4 Statelessness (PRD-NFR-001, PRD-SCH-001)
Spring services may be singletons but hold no mutable authoritative calendar state.
Request/worker-scoped scheduling state stays local to the request. PostgreSQL is the
authority for bookings and reservations.

## 3. Multi-tenancy design (ADR-002)

- **Strategy:** shared schema, `tenant_id UUID NOT NULL` on every tenant-owned table.
- **Application scoping:** `TenantContext` resolved per request from validated membership;
  repositories require an explicit `TenantId` parameter — e.g.
  `findById(TenantId, ResourceId)`. Unscoped finders for tenant-owned data are banned
  (enforced by convention + review +, where feasible, ArchUnit naming rules).
- **Database enforcement:**
  - Composite foreign keys include `tenant_id` (e.g. a `resource_assignment` references
    `(tenant_id, booking_id)` and `(tenant_id, resource_id)`), preventing cross-tenant
    references at DB level (PRD-TEN-006).
  - Tenant-scoped uniqueness includes `tenant_id` (PRD-TEN-005).
- **Never trust client tenant id** (PRD-TEN-004/SEC-002): the active tenant comes from the
  authenticated user's membership; a mismatch is rejected.
- **RLS:** optional defence-in-depth, deferred (PRD-TEN-008 / OQ-TEN-1); if enabled it
  supplements, never replaces, application scoping.

## 4. Identity & authorization (ADR-010)

- Firebase issues ID tokens; a Spring Security filter verifies the token (issuer/audience/
  signature/expiry) and resolves the `ApplicationUser` by Firebase UID.
- Active tenant resolved from a request header/claim **validated** against
  `OrganisationMember`.
- Authorization is **permission-based** (PRD-MEM-003/SEC-003): permissions derived from the
  member's roles are checked at each use-case entry point. No role-name string checks.
- Local dev: Firebase Auth emulator or a `local`-profile stub verifier (never in prod).

## 5. Data model & PostgreSQL specifics (ADR-004)

### 5.1 Identifiers
UUID primary keys (UUIDv7 preferred for index locality; see OQ-DATA-1). Typed IDs at the
Java boundary map to UUID columns.

### 5.2 Reservation integrity
- `reservation` has `during tstzrange NOT NULL` and requires the `btree_gist` extension.
- Overlap prevention via exclusion constraint:
  ```sql
  ALTER TABLE reservation
    ADD CONSTRAINT reservation_no_overlap
    EXCLUDE USING gist (
      tenant_id  WITH =,
      resource_id WITH =,
      during     WITH &&
    ) WHERE (status = 'ACTIVE');
  ```
- Confirmation runs in one transaction (PRD-BKG-004): insert booking + assignments +
  reservations; a concurrent conflict surfaces as a constraint violation mapped to a
  **409 Conflict** (PRD-BKG-006, PRD-RSV-004). No application-only check-then-insert.
- Buffers (setup/cleanup/movement, PRD-SCH-006) are folded into the reserved `during`
  range so contended time is correctly blocked.

### 5.3 Migrations (ADR-008)
Flyway plain-SQL, versioned `V<n>__<desc>.sql`, forward-only in normal operation; each
schema change ships as a migration and is exercised by a Testcontainers migration test
(PRD-NFR-005). Extensions (`btree_gist`) enabled via migration.

### 5.4 Constraints summary
- `tenant_id` NOT NULL everywhere tenant-owned.
- Composite FKs carrying `tenant_id`.
- Tenant-scoped unique constraints (e.g. resource type code unique per tenant).
- Exclusion constraint on `reservation` for overlap prevention.
- Check constraints for controlled enums where not modelled as lookup tables.

## 6. API surface (initial, internal)

REST/JSON over HTTPS; all endpoints authenticated and tenant-scoped. Indicative (subject
to Deliverable C review):
- `POST /v1/services/{serviceTypeId}/availability:search` → ranked `CandidateOption`s
  (advisory). Exposes **options**, not raw resource calendars (self-service-ready shape).
- `POST /v1/bookings` → confirm a chosen option atomically; `201` on success, `409` on
  conflict (PRD-BKG-006).
- `POST /v1/bookings/{id}:cancel|:complete|:no-show`.
- Configuration endpoints for resource types/resources/capabilities/services/availability
  (permission-gated).
- Error model: RFC-7807-style problem responses; conflict responses clearly identify the
  contended resource.

## 7. Observability & operations (PRD-NFR-002/003/004)

- Endpoints: `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`.
- Structured JSON logs with `tenant_id` and request/correlation id; no secrets/tokens/PII.
- 12-factor config via environment variables; Cloud Run-compatible container; graceful
  shutdown.

## 8. Testing strategy (PRD-NFR-006/007, DoD)

- **Domain tests** (pure, fast): capability/qualification/expiry matching, hard vs soft,
  required vs preferred, mobility, compatibility, availability intersection, buffers,
  assignment locking, exclusivity.
- **Acceptance tests**: one per user-facing PRD criterion; `@DisplayName` includes both
  `PRD-XXX-NNN` and `XXX-AT-NNN`, e.g.:
  ```java
  @Test
  @DisplayName("PRD-BKG-004 / BKG-AT-004: confirmation atomically reserves every required resource")
  void confirmationAtomicallyReservesEveryRequiredResource() { }
  ```
- **PostgreSQL integration tests** (Testcontainers): transactions, range types, exclusion
  constraints, overlapping/concurrent reservations, tenant-scoped queries, cross-tenant
  reference prevention, rollback, migrations. Not mocked repositories.
- **Architecture tests** (ArchUnit): §2.3 rules.
- **Documentation validation** (CI, PRD-NFR-008): fail when required docs missing; an
  implemented requirement lacks a traceability entry; a traceability entry references an
  unknown requirement id; a test references an unknown PRD id; an accepted-ADR link is
  broken; the decision register references a missing ADR. It does **not** attempt to prove
  prose≡code semantically.

## 9. CI (PRD-NFR-008)

Pipeline: build (Maven) → unit/domain tests → integration tests (Testcontainers) →
architecture tests → documentation validation → package container. Green CI is part of
Definition of Done.

## 10. Definition of Done (per feature)

A feature is done only when:
1. A PRD requirement with a stable ID exists.
2. Acceptance criteria are documented.
3. Implementation follows accepted ADRs.
4. Domain tests cover the business rules.
5. Acceptance tests cover the PRD criteria (dual-ID `@DisplayName`).
6. PostgreSQL integration tests cover persistence/transactional behaviour where relevant.
7. `docs/08-REQUIREMENTS-TRACEABILITY.md` updated in the **same** change.
8. This technical specification updated when contracts/architecture change.
9. `PROJECT_CONTEXT.md` updated when milestone/architecture/assumptions/key decisions change.
10. An ADR added or superseded for material decisions; decision register updated.
11. Open questions and future ideas updated where work is deferred.
12. CI passes.

Documentation is part of the deliverable, not a follow-up.

## 11. Deployment (Cloud Run)

Containerised Spring Boot app; configuration via env vars (DB URL/credentials via Secret
Manager, Firebase project id, active-profile). Stateless; scales to N instances. Readiness
gates traffic until migrations applied and DB reachable. Firebase may provide hosting and
surrounding platform capabilities for future front-ends.
