# ADR-002 — Multi-Tenant Data Model

- **Status:** ACCEPTED
- **Date:** 2026-08-01
- **Decision (register):** DEC-002, DEC-011, DEC-013
- **Related PRD:** PRD-TEN-001..008, PRD-SEC-002, PRD-IDN-004

## Context

The application is multi-tenant from the first implementation. A tenant is an organisation;
a user may belong to several. Cross-tenant data leakage is a critical failure. We must
choose an isolation strategy and enforce it in both application code and the database.

## Decision

**Shared-schema multi-tenancy** with a `tenant_id UUID NOT NULL` discriminator on every
tenant-owned table, enforced at three layers:

1. **Application scoping.** A `TenantContext` is resolved per request from **validated
   membership** (never from an unvalidated client value — PRD-TEN-004/SEC-002). Repository
   methods for tenant-owned data are **explicitly tenant-scoped**:
   `findById(TenantId, ResourceId)`. Unscoped `findById(ResourceId)` is prohibited.
2. **Database integrity.**
   - **Composite foreign keys include `tenant_id`**, making cross-tenant references
     impossible at DB level (PRD-TEN-006).
   - **Tenant-scoped uniqueness** includes `tenant_id` (PRD-TEN-005).
3. **Tests.** Cross-tenant isolation is covered by integration and acceptance tests
   (TEN-AT-001/002), including cross-tenant reference prevention via Testcontainers.

**Identifiers:** UUID primary keys, **UUIDv7** preferred (index locality), typed ID value
objects at the Java boundary (DEC-011 / OQ-DATA-1).

**Row-Level Security:** **deferred** (DEC-013 / PRD-TEN-008 / OQ-TEN-1) as optional
defence-in-depth; if later enabled it supplements, never replaces, application scoping.

## Alternatives considered

1. **Schema-per-tenant / database-per-tenant** — rejected for now: heavier operations,
   migration fan-out, connection management; revisit for large/regulated tenants
   (`06-FUTURE-IDEAS.md`).
2. **RLS as the primary isolation mechanism** — rejected as *primary*: valuable as
   defence-in-depth but must not be the only guard; explicit scoping is clearer and
   testable at the application boundary.
3. **Single `tenant_id` column with no composite FKs** — rejected: allows accidental
   cross-tenant references; composite FKs close that at the database.

## Consequences

- (+) Simple operations, single migration path, efficient shared infrastructure.
- (+) Cross-tenant references impossible at DB level; isolation testable.
- (−) Every tenant-owned query/repository must pass `TenantId` — enforced by convention,
  review and (where feasible) ArchUnit.
- (−) Composite keys add schema verbosity.
- Interacts with ADR-004 (reservation exclusion constraint includes `tenant_id`).
