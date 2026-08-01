# ADR-006 — Opinionated Defaults, Not a Meta-Model

- **Status:** ACCEPTED
- **Date:** 2026-08-01
- **Decision (register):** DEC-006, DEC-017, DEC-018
- **Related PRD:** PRD-SVC-004, PRD-RSV-002, PRD-BKG-005, PRD-SCH-005

## Context

The model must support future flexibility, but a fully dynamic meta-model or generic rules
engine would be complex, hard to reason about, and slow to ship. The product needs safe,
predictable behaviour out of the box.

## Decision

Represent flexibility through **explicit domain concepts** — resource types, capabilities,
requirements, constraints, assignments, compatibility, mobility, availability, capacity,
transition time, assignment policies — **not** a dynamic rules platform. Ship **opinionated
defaults**:

- **Exclusivity by default** (PRD-RSV-002): a resource serves at most one booking at a
  time; staff cannot serve simultaneous bookings. Exclusivity is **not weakened globally**
  to add capacity — capacity is a separate, explicit, additive model when built
  (PRD-RSV-006 / DEC-017).
- **Feasibility before confirmation** (PRD-BKG-005): a confirmed booking has valid
  assignments for every hard requirement; no partial confirmed bookings initially.
- **Hard by default** (PRD-SVC-004): safety, qualification, compatibility and availability
  constraints are hard unless explicitly made soft.
- **Simple scheduling first** (PRD-SCH-005): direct availability only; limited horizon;
  fixed increments (if useful); limited result count. No repair, no optimisation.
- **Excluded infrastructure/patterns** (DEC-018): no Redis, microservices, Kafka, event
  sourcing, CQRS, or generic rules engine without a future accepted ADR justifying need.

## Alternatives considered

1. **Dynamic meta-model / generic rules engine** — rejected: high complexity, weak
   guardrails, unclear near-term value; explicit concepts are clearer and testable.
2. **Maximally configurable everything (soft constraints tunable per field)** — rejected
   for v1: opinionated hard defaults are safer and simpler; configurability can be added
   where evidence demands.
3. **Adopt event sourcing/CQRS up front** — rejected: unjustified complexity for a modular
   monolith with PostgreSQL as authority.

## Consequences

- (+) Predictable, safe behaviour; smaller surface; faster delivery.
- (+) Extensions are explicit and ADR-gated, keeping reasoning traceable.
- (−) Some flexibility requires new explicit concepts/ADRs rather than configuration.
- (−) Teams wanting a rules engine must justify it via ADR (intentional friction).
