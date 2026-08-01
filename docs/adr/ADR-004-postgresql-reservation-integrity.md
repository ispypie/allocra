# ADR-004 — PostgreSQL Reservation Integrity

- **Status:** ACCEPTED
- **Date:** 2026-08-01
- **Decision (register):** DEC-004
- **Related PRD:** PRD-RSV-001..005, PRD-BKG-004/006, PRD-NFR-005/006

## Context

Two schedulers may try to confirm bookings that consume the same exclusive resource at the
same time. An application-level "check availability, then insert" sequence has a race window
and will double-book under concurrency. PostgreSQL is the authoritative store and can
enforce non-overlap declaratively.

## Decision

Enforce exclusive-resource non-overlap **in the database** using range types and an
exclusion constraint, inside the confirmation transaction.

- `reservation.during` is a `tstzrange` (UTC instants; see OQ-TIME-1).
- Enable the `btree_gist` extension (via Flyway migration).
- Exclusion constraint prevents overlaps for the same resource within a tenant:
  ```sql
  ALTER TABLE reservation
    ADD CONSTRAINT reservation_no_overlap
    EXCLUDE USING gist (
      tenant_id   WITH =,
      resource_id WITH =,
      during      WITH &&
    ) WHERE (status = 'ACTIVE');
  ```
- **Confirmation is one transaction** (PRD-BKG-004): revalidate availability, insert
  booking + assignments + reservations. A concurrent conflict raises a constraint
  violation, mapped to a clear **409 Conflict** (PRD-BKG-006/RSV-004).
- **No application-only check-then-insert** is relied upon (PRD-RSV-004).
- Setup/cleanup/movement **buffers** (PRD-SCH-006) are folded into `during` so contended
  time is correctly blocked.
- Availability search is **advisory** (PRD-RSV-005): a searched slot may become unavailable
  before confirmation; the confirmation API surfaces this as a conflict.

**Testing (PRD-NFR-006):** real PostgreSQL via Testcontainers for transactions, range
types, exclusion constraints, overlapping/concurrent reservations, rollback and migrations.
Mocked repositories must not replace these tests. Concurrency is exercised by
`ReservationConcurrencyIntegrationTest` (planned) proving only one of N concurrent
confirmations succeeds.

## Alternatives considered

1. **Application check-then-insert** — rejected: race window, double-booking under load.
2. **Pessimistic row locks (`SELECT … FOR UPDATE`) on resource** — workable but coarser and
   easy to get wrong across multiple resources; the exclusion constraint is declarative,
   correct by construction, and covers overlap semantics directly.
3. **Serializable isolation only** — helps but relies on retry logic and doesn't express
   overlap semantics; the exclusion constraint is the primary guard (may combine with
   sensible isolation).
4. **Redis lock** — rejected (DEC-018): adds infrastructure; DB already authoritative.

## Consequences

- (+) Double-booking is impossible regardless of application logic or instance count
  (supports statelessness/horizontal scale, PRD-NFR-001).
- (+) Correctness is declarative and testable.
- (−) Requires `btree_gist` and careful mapping of constraint violations to 409.
- (−) Buffers must be computed into ranges consistently by the application.
- (−) Capacity (non-exclusive) resources need a different model later (PRD-RSV-006) —
  must not weaken this constraint globally.
