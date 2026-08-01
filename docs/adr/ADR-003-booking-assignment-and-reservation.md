# ADR-003 — Booking, Assignment and Reservation Separation

- **Status:** ACCEPTED
- **Date:** 2026-08-01
- **Decision (register):** DEC-003, DEC-014
- **Related PRD:** PRD-BKG-001/004/005, PRD-ASN-001..004, PRD-RSV-001/003

## Context

A booking commits to delivering a service at a time. The resources that satisfy it may
change (staff swap, room change) without the booking losing its identity. Baking
`staffId`/`roomId`/`equipmentId` into the booking would block reassignment, self-service
resource selection, and future schedule repair.

## Decision

Model three distinct concepts:

- **`Booking`** — the commitment: `ServiceType`, time window, `BookingSubject`,
  `BookingStatus`, `BookingChannel`. It contains **no** resource ids.
- **`ResourceAssignment`** — binds the resource currently selected to a specific
  `ResourceRequirement` of the booking. Carries an `AssignmentPolicy`.
- **`Reservation`** — the exclusive time-range consumption of a resource, created on
  confirmation; the database-enforced double-booking guard (see ADR-004).

Rules:
- A booking is confirmable only when **fully feasible**: every hard requirement has a valid
  assignment (PRD-BKG-005).
- Confirmation atomically creates booking + assignments + one reservation per exclusive
  assigned resource, or fails entirely (PRD-BKG-004).
- An assignment may change later **without changing the `BookingId`** (PRD-ASN-002).
- Standard availability search **never silently reassigns** an existing assignment
  (PRD-ASN-004 / PRD-SCH-004).

### Future schedule repair (deferred — DEC-014, PRD-SCH-007)
When built, the default design is: proposal only; explicit scheduler approval; no
appointment-time changes; same location only; staff reassignment only; max one affected
existing booking; locked assignments must not change. Whole-schedule optimisation is out of
scope (PRD-SCH-008). Not implemented in the initial slice unless explicitly requested.

## Alternatives considered

1. **Resource ids on the booking** — rejected: no reassignment, hostile to self-service and
   repair, conflates commitment with allocation.
2. **Assignment == reservation (single concept)** — rejected: assignment is a domain
   selection that may be reassignable/locked; reservation is a DB-enforced exclusivity
   record. Keeping them separate lets assignments change while reservations guarantee
   integrity.

## Consequences

- (+) Assignments evolve without disturbing booking identity or history.
- (+) Clean seam for self-service resource selection and future schedule repair.
- (+) Reservation integrity concern isolated to ADR-004.
- (−) More entities and a multi-entity confirmation transaction to coordinate.
- (−) Feasibility must be checked before confirmation and again in the transaction.
