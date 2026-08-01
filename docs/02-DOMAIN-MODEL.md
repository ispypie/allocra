# 02 — Domain Model

This document defines the domain concepts, their relationships, aggregate boundaries and
key invariants. It is design-level (technology-agnostic); persistence detail lives in
`03-TECHNICAL-SPECIFICATION.md`. Term definitions are in `05-GLOSSARY.md`.

## Design tenets

- **Composition over inheritance.** No deep type hierarchies. Type-specific structure is
  a *profile* composed onto the generic `Resource`.
- **Typed identifiers & value objects** over raw strings/UUIDs at the domain boundary
  (`TenantId`, `ResourceId`, `BookingId`, `ServiceTypeId`, `RequirementId`,
  `AssignmentId`, `ReservationId`, `MemberId`, `UserId`, `LocationId`, …).
- **Explicit concepts over dynamic meta-model.** Flexibility is named: capabilities,
  requirements, constraints, compatibility, mobility, availability, policies.

## Bounded context / module map

| Module | Owns |
|--------|------|
| `identity` | `ApplicationUser`, Firebase token verification port |
| `tenancy` | `Tenant`, active-tenant resolution |
| `membership` | `OrganisationMember`, `Role`, `Permission` |
| `resources` | `ResourceType`, `BaseResourceKind`, `Resource`, `ResourceCapability`, `AttributeDefinition`, `EquipmentMobility`, `ResourceCompatibility`, `Location` |
| `services` | `ServiceType`, `ResourceRequirement`, `RequirementConstraint` |
| `availability` | `AvailabilityRule`, `BlockedAvailability` |
| `scheduling` | **pure** engine: snapshot inputs, constraint evaluation, candidate options |
| `bookings` | `Booking`, `BookingSubject`, `BookingStatus`, `BookingChannel` |
| `reservations` | `Reservation`, `ResourceAssignment`*, `AssignmentPolicy` |
| `audit` | `AuditEvent` |

\* `ResourceAssignment` conceptually bridges `bookings` and `reservations`. It is owned by
the booking aggregate but its exclusive consumption is realised as a `Reservation`. See
[Aggregates](#aggregates).

## Core entities

### Identity & tenancy
- **Tenant** — an organisation; root discriminator for all tenant-owned data.
- **ApplicationUser** — a system login, linked 1:1 to a Firebase UID. Global across tenants.
- **OrganisationMember** — an `ApplicationUser`'s membership of a `Tenant`, carrying
  `Role`s (and thereby `Permission`s) and participating in active-tenant resolution.
- **Role** — a named bundle of `Permission`s (Org Admin, Scheduler, Resource Member, Viewer).
- **Permission** — a fine-grained capability checked at action time (e.g.
  `BOOKING_CREATE`, `RESOURCE_MANAGE`). Authorization is by permission, never role name.

### Resources
- **Location** — a physical place within a tenant (site/building); resources and rooms
  reference locations; used by location hard constraints and movement modelling.
- **ResourceType** — tenant-defined type (staff, room, equipment, …) carrying a
  `BaseResourceKind`.
- **BaseResourceKind** — controlled enum {PERSON, PLACE, ASSET}.
- **Resource** — a schedulable item of a `ResourceType`. Generic; scheduling operates on
  this. Optional composed **profile** (e.g. `EquipmentProfile` with `EquipmentMobility`).
- **ResourceCapability** — a capability a resource possesses (qualification, skill level,
  accessibility, room privacy, equipment category, capacity, certification/expiry,
  compatibility marker). May reference an `AttributeDefinition` for structured values.
- **AttributeDefinition** — a tenant-defined typed attribute for resources/capabilities
  (bounded, explicit — not a generic rules engine).
- **EquipmentMobility** — {FIXED, MOVABLE, POOLED}; part of an equipment profile.
- **ResourceCompatibility** — allowed/forbidden combinations between resources (e.g.
  equipment ↔ room).

### Services & requirements
- **ServiceType** — a bookable service (a.k.a. TreatmentType in some verticals) with a set
  of `ResourceRequirement`s and a nominal duration.
- **ResourceRequirement** — describes *what* a service needs: target kind/type, required
  capabilities, required vs optional, quantity (initially 1), and staff selection mode
  (`REQUIRED`/`PREFERRED`/`ANY`).
- **RequirementConstraint** — a constraint on a requirement, classified **hard** or
  **soft**. Hard = validity; soft = ranking. Safety/qualification/compatibility/
  availability are hard by default.

### Availability
- **AvailabilityRule** — recurring/defined windows when a resource is available.
- **BlockedAvailability** — explicit unavailability overriding rules.

### Bookings, subjects, assignments, reservations
- **BookingSubject** — the person/organisation/asset **receiving** the service (type,
  display name, email, phone, external reference). Neutral — not `Customer`, not a CRM.
- **Booking** — a commitment to provide a `ServiceType` at a time window for a
  `BookingSubject`, with `BookingStatus` and `BookingChannel`. **Holds no resource ids.**
- **BookingStatus** — {CONFIRMED, CANCELLED, COMPLETED, NO_SHOW}.
- **BookingChannel** — {INTERNAL, SELF_SERVICE, API, IMPORT}.
- **ResourceAssignment** — the `Resource` currently selected to satisfy a specific
  `ResourceRequirement` of a specific `Booking`. Carries an `AssignmentPolicy`.
- **AssignmentPolicy** — {REASSIGNABLE, LOCKED, LOCKED_BY_POLICY} (behaviour beyond
  REASSIGNABLE-at-confirmation deferred).
- **Reservation** — an exclusive time-range consumption of a `Resource`, created when a
  booking is confirmed; the database-enforced double-booking guard.

### Audit
- **AuditEvent** — tenant-scoped record of a significant action (actor, action, target,
  timestamp, context).

## Aggregates & consistency boundaries

- **Booking aggregate** = `Booking` + its `ResourceAssignment`s (+ `BookingSubject` as a
  value-ish entity owned by the booking). Invariant: a `CONFIRMED` booking has a valid
  assignment for **every hard requirement** of its service (PRD-BKG-005).
- **Reservation** is a separate consistency concern enforced by the database. Confirmation
  is a **single transaction** spanning the booking aggregate and its reservations
  (PRD-BKG-004): create booking + assignments + one reservation per exclusive assigned
  resource, or roll back entirely.
- **Resource / ResourceType / ServiceType / AvailabilityRule** are configuration
  aggregates edited independently of bookings.

## Key relationships (textual ER)

```
Tenant 1───* OrganisationMember *───1 ApplicationUser (1:1 Firebase UID)
OrganisationMember *───* Role *───* Permission
Tenant 1───* Location
Tenant 1───* ResourceType (has BaseResourceKind) 1───* Resource
Resource *───1 Location (optional home/current)
Resource 1───* ResourceCapability (may ref AttributeDefinition)
Resource *───* Resource   (ResourceCompatibility)
Tenant 1───* ServiceType 1───* ResourceRequirement 1───* RequirementConstraint
Resource 1───* AvailabilityRule
Resource 1───* BlockedAvailability
Tenant 1───* Booking 1───1 BookingSubject
Booking (ServiceType) 1───* ResourceAssignment *───1 Resource
ResourceAssignment 1───1 Reservation   (per exclusive assigned resource)
Tenant 1───* AuditEvent
```

## Invariants (must always hold)

1. Every tenant-owned entity has a `tenant_id`; no cross-tenant reference exists (PRD-TEN-*).
2. A `Booking` contains no `staffId`/`roomId`/`equipmentId` (PRD-BKG-001).
3. A `CONFIRMED` booking satisfies all hard requirements via valid assignments (PRD-BKG-005).
4. Each confirmed exclusive assignment has exactly one active `Reservation` (PRD-RSV-003).
5. No two active reservations for the same resource overlap in time (PRD-RSV-004).
6. Changing an assignment does not change the `BookingId` (PRD-ASN-002).
7. Scheduling produces candidates without mutating any aggregate (PRD-SCH-001).
8. A requirement describes needed capabilities, not a specific resource, unless the
   request explicitly requires one (PRD-SVC-002/005).

## Scheduling engine inputs/outputs (pure)

**Input — SchedulingSnapshot (immutable):** the service's requirements & constraints; the
candidate resources with capabilities, profiles, compatibility, availability rules, blocks;
existing reservations in the window; the requested window/subject/preferences.
**Output — ranked `CandidateOption`s:** for each option, a proposed
requirement→resource mapping with feasibility (all hard constraints satisfied) and a soft
score. The engine imports **no** infrastructure, HTTP, Firebase or JPA types.

## Notes on flexibility (what we deliberately did *not* build)

- No generic rules engine — constraints are typed hard/soft evaluators.
- No dynamic entity meta-model — `AttributeDefinition` is bounded and explicit.
- No inheritance tree for resource types — composition via profiles.
- No capacity/pooled behaviour yet — modelled only where shape is needed; exclusivity is
  the default and is not globally weakened.
