# 05 — Glossary

Canonical definitions. Use these terms consistently in code, tests and docs.

| Term | Definition |
|------|-----------|
| **Tenant** | An organisation; the root discriminator (`tenant_id`) for all tenant-owned data. |
| **ApplicationUser** | A system login, linked 1:1 to a Firebase UID; global across tenants. Distinct from a person `Resource`. |
| **OrganisationMember** | An `ApplicationUser`'s membership of a `Tenant`, carrying roles/permissions; basis for active-tenant resolution. |
| **Role** | A named bundle of permissions (Org Admin, Scheduler, Resource Member, Viewer). |
| **Permission** | A fine-grained capability checked at action time (e.g. `BOOKING_CREATE`). Authorization is by permission, never role name. |
| **Active tenant** | The single tenant a request operates in, derived from validated membership — never trusted from the client. |
| **Location** | A physical place within a tenant; referenced by resources/rooms and location constraints. |
| **Resource** | Any schedulable item. The generic abstraction the scheduling engine operates on. |
| **ResourceType** | A tenant-defined type of resource (staff, room, equipment, …) carrying a `BaseResourceKind`. |
| **BaseResourceKind** | Controlled enum: `PERSON`, `PLACE`, `ASSET`. |
| **ResourceCapability** | A capability a resource possesses (qualification, level, accessibility, category, capacity, expiry, compatibility). |
| **AttributeDefinition** | A tenant-defined typed attribute for resources/capabilities. Bounded and explicit — not a generic rules engine. |
| **EquipmentMobility** | Equipment property: `FIXED`, `MOVABLE`, `POOLED`. |
| **ResourceCompatibility** | Allowed/forbidden combinations between resources (e.g. equipment usable in a room). |
| **Profile** | Type-specific structural data composed onto a `Resource` (e.g. `EquipmentProfile`). Composition, not inheritance. |
| **ServiceType** | A bookable service (a.k.a. TreatmentType) defined by its resource requirements and a duration. |
| **ResourceRequirement** | Describes *what* a service needs (kind/type + capabilities), required vs optional, staff selection mode. Not a named resource unless explicitly required. |
| **RequirementConstraint** | A constraint on a requirement, classified **hard** (validity) or **soft** (ranking). |
| **Hard constraint** | Determines whether an option is valid (availability, qualification, compatibility, location, reservation conflicts). Hard by default for safety/qualification/compatibility/availability. |
| **Soft constraint** | Ranks otherwise-valid options (preferred practitioner/room, continuity, reduced movement, fewer gaps, workload balance). |
| **Staff selection mode** | Per staff requirement/request: `REQUIRED` (hard), `PREFERRED` (soft), `ANY`. |
| **BookingSubject** | The person/organisation/asset **receiving** the service. Neutral term — not `Customer`, not a CRM record. |
| **Booking** | A commitment to provide a `ServiceType` at a time window for a `BookingSubject`. Holds **no** resource ids. |
| **BookingStatus** | `CONFIRMED`, `CANCELLED`, `COMPLETED`, `NO_SHOW`. |
| **BookingChannel** | `INTERNAL`, `SELF_SERVICE`, `API`, `IMPORT`. Only `INTERNAL` delivered initially. |
| **ResourceAssignment** | The resource currently selected to satisfy a specific requirement of a specific booking. |
| **AssignmentPolicy** | `REASSIGNABLE`, `LOCKED`, `LOCKED_BY_POLICY` (behaviour beyond confirmation-time reassignable is deferred). |
| **Reservation** | An exclusive time-range consumption of a resource, created on confirmation; the DB-enforced double-booking guard. |
| **AvailabilityRule** | Defined/recurring windows when a resource is available. |
| **BlockedAvailability** | Explicit unavailability overriding availability rules. |
| **Effective availability** | `AvailabilityRule`s minus `BlockedAvailability` minus conflicting `Reservation`s. |
| **AuditEvent** | Tenant-scoped record of a significant action (actor, action, target, time, context). |
| **Direct availability search** | Finds options without changing existing bookings/assignments. Advisory. |
| **Schedule repair** | (Deferred) Proposal-only reassignment to make a new booking possible, under strict constraints. |
| **SchedulingSnapshot** | Immutable input to the pure scheduling engine (requirements, resources, availability, existing reservations, request). |
| **CandidateOption** | A ranked, feasible requirement→resource mapping returned by the engine. |
| **Modular monolith** | Single deployable composed of explicitly-bounded modules. |
| **Exclusivity** | Default rule that a resource serves at most one booking at a time. Not weakened globally to add capacity. |
| **Feasible booking** | A booking whose every hard requirement has a valid assignment; required before confirmation. |
| **Tenant scoping** | Passing/validating `TenantId` on every access to tenant-owned data. |
