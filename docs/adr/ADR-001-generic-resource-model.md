# ADR-001 — Generic Resource Model

- **Status:** ACCEPTED
- **Date:** 2026-08-01
- **Decision (register):** DEC-001
- **Related PRD:** PRD-RES-001..005, PRD-SCH-001

## Context

The platform must schedule heterogeneous things — staff, rooms, equipment today; vehicles,
inspection bays, desks, machines, tooling, treatment stations tomorrow. If each type has
its own scheduling model, every new type forces schema and engine changes, and the engine
accretes type-specific branches.

## Decision

Every schedulable item is a generic **`Resource`**. Each resource has a **`ResourceType`**
carrying a controlled **`BaseResourceKind`** ∈ {PERSON, PLACE, ASSET}. The scheduling
engine operates only on the generic model plus **capabilities**, **requirements**,
**constraints**, **compatibility**, **mobility** and **availability**.

- Type-specific structural data is modelled as a **profile composed onto** the resource
  (e.g. `EquipmentProfile`), **not** via an inheritance hierarchy.
- Adding a new resource type must require **no change** to the scheduling engine
  (verified by acceptance test RES-AT-001 and ArchUnit rules).
- Domain-specific schedulers that read staff/room/equipment directly are prohibited.

## Alternatives considered

1. **Separate models/schedulers per type** — rejected: duplication, closed to new types,
   branching engine.
2. **Deep inheritance (`StaffResource extends Resource`, …)** — rejected: rigid, fragile,
   poor fit for cross-cutting capabilities; composition chosen instead.
3. **Fully dynamic meta-model** — rejected (see ADR-006): complexity without near-term value.

## Consequences

- (+) New resource types are configuration, not code.
- (+) One engine to test and reason about.
- (+) Capabilities/constraints are uniform across types.
- (−) Requires discipline: type-specific needs must be expressed as capabilities/profiles,
  not special-cased in the engine (enforced by ArchUnit + review).
- Enables: generic availability search, uniform reservation model (ADR-003/004).
