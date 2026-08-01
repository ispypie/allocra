# PROJECT_CONTEXT.md

> **This is the first document every human or AI contributor must read.**
> It is a concise orientation and index — not a duplicate of the other documents.
> When it disagrees with a more specific document, follow the precedence order in
> [§ Conflict precedence](#conflict-precedence).

---

## Contributor rule (read this every time)

> **Before making a material change**, read `PROJECT_CONTEXT.md`, the relevant PRD
> sections, relevant technical specification sections and related accepted ADRs.
> **After making a material change**, update the affected documentation, requirements
> traceability and decision records.

A "material change" is any change to behaviour, architecture, the domain model, the
data model, public contracts, dependencies, or accepted decisions. See
[docs/03-TECHNICAL-SPECIFICATION.md](docs/03-TECHNICAL-SPECIFICATION.md) §"Definition of Done"
for the full checklist. **Documentation is part of the deliverable, not a follow-up task.**

After each meaningful delivery, state whether `PROJECT_CONTEXT.md` required an update
and what changed.

---

## Product purpose

**Allocra** is a **multi-tenant resource scheduling platform**. Organisations define
**bookable services** and the platform allocates the required combination of **people,
places and assets** to satisfy each booking — e.g. a treatment needing a qualified
staff member, a suitable room and specialist equipment; a vehicle inspection needing an
inspector, a bay and diagnostic equipment.

The initial product supports **authenticated internal booking** by members of an
organisation. A **public self-service** channel is expected soon; the model must support
it **without redesign**, but no public UI is built in this phase.

## Current milestone

**Milestone 1 — Project elaboration & foundation.**
- **Deliverable A — documentation: DONE.**
- **Deliverable B — repository & engineering foundation: DONE.** Maven multi-module
  reactor (12 modules), Spring Boot 3.4 bootstrap, PostgreSQL + Flyway (`V1` baseline with
  `btree_gist` + `tenant`), Testcontainers ITs, JUnit, ArchUnit boundary tests, Spotless
  formatting, SpotBugs (opt-in `-Pquality`), actuator health/liveness/readiness, structured
  logging (cloud profile), Cloud Run `Dockerfile`, and GitHub Actions CI (incl.
  documentation validation). **CI is green** on JDK 21 (build job: unit + ArchUnit + doc +
  Testcontainers integration tests + Spotless; static-analysis job: SpotBugs). Repo:
  `github.com/ispypie/allocra` (private). See `README.md`.
- **Deliverable C — thin vertical slice *plan*: DONE (design, awaiting review).** See
  [docs/09-VERTICAL-SLICE-PLAN.md](docs/09-VERTICAL-SLICE-PLAN.md). Slice code is **not**
  written until the plan is reviewed.
- **Deliverable D — acceptance test plan: DONE.** See
  [docs/10-ACCEPTANCE-TEST-PLAN.md](docs/10-ACCEPTANCE-TEST-PLAN.md).

Next action awaiting the maintainer: **review Deliverables C & D**, resolve the two open
decisions (buffers in-slice? real Firebase filter in-slice?), then implement the slice TDD.

**We are not implementing the full product.** Do not build broad functionality until
the foundation plan is reviewed.

## Current architecture

- **Modular monolith** deployable to **Google Cloud Run** (stateless, horizontally scalable).
- Modules: `common`, `identity`, `tenancy`, `membership`, `resources`, `services`,
  `availability`, `scheduling`, `bookings`, `reservations`, `audit` (+ `app` bootstrap).
- Each module is layered `domain` (pure) / `application` (services & ports) / `adapter`
  (JPA, web). The **`scheduling` domain is pure** — no HTTP, Spring MVC, Firebase SDK,
  JPA entities, or deployment specifics.
- **PostgreSQL is the authority** for bookings and reservations. The scheduling engine
  computes candidates in memory from an **immutable snapshot** and mutates nothing.

## Core domain concepts

`Tenant`, `ApplicationUser`, `OrganisationMember`, `Role`, `Permission`, `Location`,
`ResourceType`, `BaseResourceKind` (PERSON/PLACE/ASSET), `Resource`, `ResourceCapability`,
`AttributeDefinition`, `ServiceType`, `ResourceRequirement`, `RequirementConstraint`,
`BookingSubject`, `Booking`, `ResourceAssignment`, `Reservation`, `AvailabilityRule`,
`BlockedAvailability`, `BookingChannel`, `BookingStatus`, `EquipmentMobility`,
`ResourceCompatibility`, `AssignmentPolicy`, `AuditEvent`.
Full definitions: [docs/02-DOMAIN-MODEL.md](docs/02-DOMAIN-MODEL.md) and
[docs/05-GLOSSARY.md](docs/05-GLOSSARY.md).

## Accepted design principles (do not violate without a new/superseding ADR)

1. **Generic resource model** — every schedulable item is a `Resource`; new resource
   types must not require changes to the scheduling engine. ([ADR-001](docs/adr/ADR-001-generic-resource-model.md))
2. **Multi-tenant from day one** — `tenant_id` on all tenant-owned data; repositories
   are explicitly tenant-scoped; never trust a client-supplied tenant id.
   ([ADR-002](docs/adr/ADR-002-multi-tenant-data-model.md))
3. **Booking / Assignment / Reservation separation** — a `Booking` has **no**
   `staffId`/`roomId`/`equipmentId`; assignments can change without changing booking
   identity. ([ADR-003](docs/adr/ADR-003-booking-assignment-and-reservation.md))
4. **PostgreSQL reservation integrity** — overlap prevention via range types + exclusion
   constraint inside a transaction; no application-only check-then-insert.
   ([ADR-004](docs/adr/ADR-004-postgresql-reservation-integrity.md))
5. **Internal-first, self-service-ready** — build internal booking only; never assume
   actor == subject == provider, or that all bookings are confirmed/public.
   ([ADR-005](docs/adr/ADR-005-internal-first-self-service-ready.md))
6. **Opinionated defaults, not a meta-model** — flexibility via explicit domain concepts,
   not a dynamic rules engine. ([ADR-006](docs/adr/ADR-006-opinionated-defaults.md))
7. **Stateless scheduling services** — no mutable authoritative calendar state in process.

## Current technology choices

| Area | Choice | ADR |
|------|--------|-----|
| Backend language | **Java 21 (LTS)** | [ADR-009](docs/adr/ADR-009-runtime-java-and-spring-boot.md) |
| Framework | **Spring Boot 3.x** | [ADR-009](docs/adr/ADR-009-runtime-java-and-spring-boot.md) |
| Build tool | **Maven** | [ADR-007](docs/adr/ADR-007-build-tool-maven.md) |
| Datastore | **PostgreSQL** (authoritative) | [ADR-002](docs/adr/ADR-002-multi-tenant-data-model.md), [ADR-004](docs/adr/ADR-004-postgresql-reservation-integrity.md) |
| Migrations | **Flyway** (plain SQL) | [ADR-008](docs/adr/ADR-008-migration-tool-flyway.md) |
| Identity | **Firebase Authentication** | [ADR-010](docs/adr/ADR-010-firebase-authentication.md) |
| Integration tests | **Testcontainers** (real PostgreSQL) | [ADR-004](docs/adr/ADR-004-postgresql-reservation-integrity.md) |
| Deployment | **Google Cloud Run** | [ADR-005](docs/adr/ADR-005-internal-first-self-service-ready.md) |

## Assumptions currently in force

See [docs/07-OPEN-QUESTIONS.md](docs/07-OPEN-QUESTIONS.md) for the authoritative list.
Headline assumptions:
- UUID (v7) primary keys; typed ID value objects at the Java boundary.
- Modules are enforced by package structure and ArchUnit within a Maven multi-module reactor.
- Firebase ID tokens are verified in a Spring Security filter; local dev may use the
  Firebase Auth emulator or a stub verifier profile.
- Row-Level Security is documented as optional defence-in-depth, **not** a replacement
  for explicit application-level tenant scoping.

## Known constraints

- Do **not** introduce Redis, microservices, Kafka, event sourcing, CQRS or a generic
  rules engine without an accepted ADR justifying it.
- Backend must be **stateless** and horizontally scalable.
- Resources are **exclusive by default**; do not weaken exclusivity globally to add capacity.
- A confirmed booking must be **fully feasible** (all hard requirements assigned) — no
  partially-assigned confirmed bookings initially.

## Current open questions

Tracked in [docs/07-OPEN-QUESTIONS.md](docs/07-OPEN-QUESTIONS.md). None currently block
the foundation milestone. Highest-impact: search horizon/increment defaults (SCH),
whether to enable RLS in phase 1, and UUID vs bigint identifier strategy (defaulted to UUIDv7).

## Explicit non-goals (this phase)

- Public self-service booking **UI**.
- Schedule repair / reassignment of existing bookings during search.
- Whole-schedule optimisation.
- Temporary booking holds.
- Capacity/pooled resources beyond model *shape*.
- Redis, microservices, Kafka, event sourcing, CQRS, generic rules engine.
- CRM features on `BookingSubject`.

## Documentation map

| File | Purpose |
|------|---------|
| [docs/00-VISION.md](docs/00-VISION.md) | Product vision & north star |
| [docs/01-PRD.md](docs/01-PRD.md) | Product requirements, personas, scope, requirement IDs |
| [docs/02-DOMAIN-MODEL.md](docs/02-DOMAIN-MODEL.md) | Domain concepts, aggregates, relationships |
| [docs/03-TECHNICAL-SPECIFICATION.md](docs/03-TECHNICAL-SPECIFICATION.md) | Architecture, data model, contracts, DoD |
| [docs/04-DECISION-REGISTER.md](docs/04-DECISION-REGISTER.md) | All significant decisions |
| [docs/05-GLOSSARY.md](docs/05-GLOSSARY.md) | Canonical term definitions |
| [docs/06-FUTURE-IDEAS.md](docs/06-FUTURE-IDEAS.md) | Deferred features & ideas |
| [docs/07-OPEN-QUESTIONS.md](docs/07-OPEN-QUESTIONS.md) | Open questions & recorded assumptions |
| [docs/08-REQUIREMENTS-TRACEABILITY.md](docs/08-REQUIREMENTS-TRACEABILITY.md) | Requirement → code → test mapping |
| [docs/09-VERTICAL-SLICE-PLAN.md](docs/09-VERTICAL-SLICE-PLAN.md) | Thin internal-booking vertical slice design (Deliverable C) |
| [docs/10-ACCEPTANCE-TEST-PLAN.md](docs/10-ACCEPTANCE-TEST-PLAN.md) | Slice → PRD → acceptance-test mapping (Deliverable D) |
| [docs/adr/](docs/adr/) | Architecture Decision Records (ADR-001 … ADR-010) |

## Conflict precedence

If documents conflict, resolve in this order, then **surface and record** the resolution:
1. Latest **accepted ADR**
2. Latest **accepted PRD decision**
3. **Technical specification**
4. **PROJECT_CONTEXT.md**
5. **Implementation**
6. **Code comments**

## Keeping documentation current

For any material change: add/adjust a PRD requirement (stable ID) **before** implementing;
follow accepted ADRs; add domain + acceptance + (where relevant) PostgreSQL integration
tests; update `docs/08-REQUIREMENTS-TRACEABILITY.md` **in the same change**; update the
technical spec when contracts/architecture change; update this file when the milestone,
architecture, assumptions or key decisions change; add or supersede an ADR for material
decisions and update the decision register; update open questions and future ideas for
deferred work. Never rename/reuse requirement or decision IDs — mark superseded and link
forward.
