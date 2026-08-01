# 06 — Future Ideas (Deferred)

Deferred work and directional ideas. Presence here is **not** a commitment. Anything that
becomes real needs a PRD requirement, a decision-register entry, and (if material) an ADR.

## Public self-service channel (near-term direction)
Ref: PRD §12, ADR-005, PRD-BKG-003.
- Publication of selected services; public tenant identity/slug.
- Public availability search exposing **bookable options, not internal calendars**.
- Restricted exposure of resource details (no staff schedules).
- Customer-created bookings; optional approval; not all immediately confirmed.
- Booking **holds** (temporary pre-confirmation reservations) — PRD-BKG-007.
- Cancellation/rescheduling policies; notifications; optional customer resource selection.
- Public UI (separate front-end; Firebase hosting candidate).

## Schedule repair (constrained, proposal-only)
Ref: PRD-SCH-007, DEC-014, ADR-003 §future.
Default future design: proposal only; explicit scheduler approval; no appointment-time
changes; same location only; staff reassignment only; max one affected existing booking;
locked assignments must not change. Whole-schedule optimisation (PRD-SCH-008) remains out
of scope.

## Capacity & pooled resources
Ref: PRD-RSV-006, PRD-RES-007, DEC-017.
Explicit capacity model for non-exclusive resources (e.g. a room seating N, a pool of
interchangeable devices). Must be additive; **must not** weaken global exclusivity.
POOLED equipment selection from an interchangeable set.

## Movement / setup / cleanup modelling (full)
Ref: PRD-RES-007, PRD-SCH-006.
MOVABLE equipment with home/current location, compatible destinations, movement/setup/
cleanup times folded into reservation windows and feasibility; fixed-equipment room binding.

## Richer qualification & supervision
Ref: PRD-SVC-007.
Qualification alternatives (any-of sets), supervision requirements, competency ladders,
role-based coverage rules.

## Assignment lifecycle behaviours
Ref: PRD-ASN-003.
Reassignment workflows, explicit locking, policy-driven locking, reassignment audit and
notifications.

## Tenancy strategy alternatives
Ref: ADR-002.
Schema-per-tenant or database-per-tenant for large/regulated tenants; per-tenant
encryption; data residency. Currently shared-schema with `tenant_id`.

## Row-Level Security as defence-in-depth
Ref: PRD-TEN-008, DEC-013, OQ-TEN-1.
Enable PostgreSQL RLS policies keyed on a session `tenant_id`, supplementing (not
replacing) application scoping.

## Platform & integration
- API channel hardening (PRD-BKG-003 `API`) and import pipelines (`IMPORT`).
- Notifications (email/SMS/push) and reminders.
- Calendar sync (Google/Microsoft) for staff resources.
- Reporting/analytics; utilisation and workload dashboards.
- Idempotency keys for confirmation; outbox pattern if async needed (ADR-gated).

## Explicitly rejected for now (need an ADR to revisit)
Redis, microservices, Kafka, event sourcing, CQRS, generic rules engine (DEC-018);
turning `BookingSubject` into a CRM (PRD-SUB-002).
