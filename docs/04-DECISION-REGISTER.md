# 04 — Decision Register

Concise record of all significant product and technical decisions. ADRs hold the full
reasoning for decisions that materially affect architecture or the domain model.

**Statuses:** `PROPOSED`, `ACCEPTED`, `DEFERRED`, `REJECTED`, `SUPERSEDED`.
**Rule:** never rewrite history. When a decision changes, mark the old one `SUPERSEDED` and
link to its replacement.

| ID | Topic | Decision | Status | Date | Related PRD | ADR |
|----|-------|----------|--------|------|-------------|-----|
| DEC-001 | Generic resource model | Every schedulable item is a generic `Resource` with `BaseResourceKind`; new types need no engine change; composition over inheritance | ACCEPTED | 2026-08-01 | PRD-RES-001..005 | ADR-001 |
| DEC-002 | Multi-tenant data model | Shared schema, `tenant_id` on all tenant-owned data, explicit tenant-scoped repositories, composite FKs incl. tenant, tenant-scoped uniqueness; never trust client tenant id | ACCEPTED | 2026-08-01 | PRD-TEN-001..007, PRD-SEC-002 | ADR-002 |
| DEC-003 | Booking/assignment/reservation separation | Booking has no resource ids; assignments bind resources to requirements; reservations enforce exclusive consumption; assignments changeable without changing booking identity | ACCEPTED | 2026-08-01 | PRD-BKG-001, PRD-ASN-*, PRD-RSV-* | ADR-003 |
| DEC-004 | Reservation integrity in PostgreSQL | Overlap prevention via `tstzrange` + GiST exclusion constraint inside a transaction; no app-only check-then-insert; concurrent conflict → 409 | ACCEPTED | 2026-08-01 | PRD-RSV-004, PRD-BKG-004/006 | ADR-004 |
| DEC-005 | Internal-first, self-service-ready | Deliver internal booking only; keep model free of actor==subject==provider and all-confirmed/all-public assumptions; expose options not calendars | ACCEPTED | 2026-08-01 | PRD-BKG-003, §12, PRD-SUB-* | ADR-005 |
| DEC-006 | Opinionated defaults, not a meta-model | Flexibility via explicit domain concepts with safe defaults; no dynamic rules engine | ACCEPTED | 2026-08-01 | PRD-SVC-004, PRD-RSV-002, PRD-BKG-005 | ADR-006 |
| DEC-007 | Build tool | Use **Maven** multi-module reactor (ubiquity/predictability for future human & AI contributors) | ACCEPTED | 2026-08-01 | PRD-NFR-008 | ADR-007 |
| DEC-008 | Migration tool | Use **Flyway** plain SQL (faithful PostgreSQL DDL: range types, exclusion constraints, RLS) | ACCEPTED | 2026-08-01 | PRD-NFR-005 | ADR-008 |
| DEC-009 | Runtime & framework | **Java 21 (LTS)** + **Spring Boot 3.x** | ACCEPTED | 2026-08-01 | PRD-NFR-009 | ADR-009 |
| DEC-010 | Authentication | **Firebase Authentication** for global identity; PostgreSQL owns app-user/membership/roles/permissions; token verified in a security filter; permission-based authz | ACCEPTED | 2026-08-01 | PRD-IDN-*, PRD-SEC-*, PRD-MEM-003 | ADR-010 |
| DEC-011 | Identifiers | UUID PKs, **UUIDv7** preferred; typed ID value objects at Java boundary | ACCEPTED (assumption) | 2026-08-01 | — | ADR-002 §5.1 / OQ-DATA-1 |
| DEC-012 | Module enforcement | Package-layered modules within a Maven reactor, boundaries enforced by ArchUnit | ACCEPTED | 2026-08-01 | PRD-NFR-007 | ADR-007 |
| DEC-013 | Row-Level Security | Treat RLS as optional defence-in-depth; **deferred**, not a replacement for app scoping | DEFERRED | 2026-08-01 | PRD-TEN-008 | ADR-002 / OQ-TEN-1 |
| DEC-014 | Schedule repair | Direct availability only initially; schedule repair deferred; future design = proposal-only, approval-gated, same-location, staff-reassignment-only, max one affected booking, locked assignments untouched | DEFERRED | 2026-08-01 | PRD-SCH-007 | ADR-003 §future |
| DEC-015 | Whole-schedule optimisation | Out of scope for initial release | DEFERRED | 2026-08-01 | PRD-SCH-008 | — |
| DEC-016 | Booking holds | Deferred until self-service requires them | DEFERRED | 2026-08-01 | PRD-BKG-007 | ADR-005 |
| DEC-017 | Capacity/pooled resources | Model shape permitted; behaviour deferred; exclusivity not weakened globally | DEFERRED | 2026-08-01 | PRD-RSV-006, PRD-RES-007 | ADR-006 |
| DEC-018 | No Redis / microservices / Kafka / CQRS / event-sourcing / rules-engine | Excluded initially; require an accepted ADR to introduce | ACCEPTED | 2026-08-01 | PRD-NFR-001 | ADR-006 |
| DEC-019 | Engineering quality gates | Formatting via Spotless (Eclipse JDT formatter, portable across JDKs); static analysis via SpotBugs (opt-in `-Pquality`, run in CI on JDK 21); structured logging via Spring Boot built-in ECS format in the `cloud` profile; integration tests split as failsafe `*IT` | ACCEPTED | 2026-08-01 | PRD-NFR-004/006/007/008 | ADR-007 |

| DEC-020 | Slice authentication | Pluggable auth-verification **port** (in `identity`) with a **local stub verifier** for tests/local dev; real Firebase adapter deferred behind a non-local profile. Keeps the slice fully locally runnable; Firebase SDK stays out of the domain | ACCEPTED | 2026-08-01 | PRD-IDN-001, PRD-SEC-001, OQ-AUTH-1 | ADR-010 |
| DEC-021 | Buffers deferred from slice | The first slice reserves exact `[start, start+duration)` windows; setup/cleanup buffers (PRD-SCH-006, RES-AT-005) deferred to the next milestone to keep the slice thin | ACCEPTED | 2026-08-01 | PRD-SCH-006, OQ-BUF-1 | ADR-006 |

## Change log
- 2026-08-01 — Initial register created with DEC-001..018 during Milestone 1 Deliverable A.
- 2026-08-01 — Added DEC-019 (engineering quality gates) during Milestone 1 Deliverable B.
- 2026-08-01 — Added DEC-020 (slice auth: stub verifier) and DEC-021 (buffers deferred) at start of slice implementation.
