# ADR-008 — Migration Tool: Flyway

- **Status:** ACCEPTED
- **Date:** 2026-08-01
- **Decision (register):** DEC-008
- **Related PRD:** PRD-NFR-005, PRD-RSV-004, PRD-BKG-004

## Context

The instruction requires choosing and recording Flyway or Liquibase. Our schema depends on
**PostgreSQL-specific features**: range types (`tstzrange`), the `btree_gist` extension, a
GiST **exclusion constraint** for reservation non-overlap (ADR-004), composite foreign keys
including `tenant_id` (ADR-002), and possibly Row-Level Security later. We need migrations
that express raw PostgreSQL DDL faithfully.

## Decision

Use **Flyway** with **plain SQL** migrations (`V<n>__<description>.sql`).

- Migrations live on the classpath (under `app`/`db`) and run at startup (or via CI/deploy),
  gating readiness (PRD-NFR-002/003).
- Forward-only in normal operation; every schema change ships as a migration (PRD-NFR-005).
- PostgreSQL extensions (`btree_gist`) and advanced constraints are declared directly in SQL.
- Each migration is exercised by a **Testcontainers** migration test on real PostgreSQL
  (PRD-NFR-006), and the exclusion-constraint behaviour is covered by reservation
  integration tests.

## Alternatives considered

1. **Liquibase (XML/YAML/JSON changelogs)** — database-agnostic abstraction is a poor fit
   when we deliberately rely on PostgreSQL-specific DDL; the abstraction adds friction for
   exclusion constraints, range types and RLS. Liquibase can run raw SQL too, at which point
   Flyway's simpler plain-SQL model wins for our needs.

## Consequences

- (+) Full-fidelity PostgreSQL DDL; no abstraction fighting exclusion constraints/range
  types/RLS.
- (+) Simple, readable, reviewable SQL; easy to test with Testcontainers.
- (−) Not database-portable (acceptable: PostgreSQL is the authoritative, chosen datastore).
- (−) No built-in rollback DSL; we manage forward migrations and rely on tests + backups.
