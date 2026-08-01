# 07 — Open Questions & Recorded Assumptions

Open questions and the assumptions currently in force. **None of the open questions below
block the foundation milestone** (per §21 handling-uncertainty: use accepted defaults,
record assumptions, continue non-blocked work). Each open question has a working default
so work is not blocked.

**Status values:** `OPEN`, `ASSUMED` (default chosen, revisit later), `RESOLVED`.

## Open questions

| ID | Question | Working default (assumption) | Status | Impact |
|----|----------|------------------------------|--------|--------|
| OQ-SCH-1 | Search horizon, increment granularity and max result count for direct availability search? | Horizon 14 days; 15-minute increments; max 50 candidate options. Tunable per tenant later. | ASSUMED | Medium — affects UX & performance; not architectural. |
| OQ-DATA-1 | UUIDv7 vs bigint identity strategy? | UUIDv7 primary keys (index locality + externalisable ids) with typed ID wrappers. | ASSUMED | Low-medium — reversible with a migration. |
| OQ-TEN-1 | Enable PostgreSQL RLS in phase 1 as defence-in-depth? | Do **not** enable in phase 1; rely on explicit app scoping + composite FKs; revisit before public channel. | ASSUMED | Medium — additive later; not blocking. |
| OQ-MOD-1 | Full Maven module-per-domain now, or package-enforced modules within fewer artifacts? | Package-layered modules within a Maven reactor; isolate `scheduling` and `common` as their own modules at minimum; enforce with ArchUnit. | ASSUMED | Low — refactorable. |
| OQ-AVL-1 | Availability rule model: RRULE/iCal recurrence vs simple weekly templates + exceptions? | Start with weekly templates + `BlockedAvailability` exceptions; defer full recurrence. | ASSUMED | Medium — model shape; keep rule storage flexible. |
| OQ-TIME-1 | Time zone handling for bookings across locations? | Store instants in UTC (`timestamptz`); resolve display/booking-window in the tenant/location time zone; ranges use `tstzrange`. | ASSUMED | Medium — get right early; encoded in tech spec. |
| OQ-SVC-1 | Do multiple resources per requirement (quantity > 1) ship in the slice? | Quantity fixed at 1 for the slice; model allows quantity for later. | ASSUMED | Low. |
| OQ-BUF-1 | Are setup/cleanup buffers in the first vertical slice or deferred? | Model supports buffers; slice implements only what its acceptance tests need (RES-AT-005 planned); may fold in during slice review. | ASSUMED | Low. |
| OQ-AUTH-1 | Local-dev auth: Firebase Auth emulator vs stub verifier profile? | Support both; default `local` profile uses a stub verifier; emulator optional. Never in prod. | ASSUMED | Low. |
| OQ-API-1 | Idempotency for confirmation requests (client retries)? | Out of slice; recommend idempotency key before public/API channel. | OPEN | Medium (future). |

## Assumptions currently in force (summary)

- Java 21 LTS; Spring Boot 3.x; Maven; Flyway; PostgreSQL; Firebase Auth; Testcontainers;
  Cloud Run (all ADR-backed — see decision register).
- UUIDv7 identifiers with typed ID value objects (OQ-DATA-1).
- Shared-schema multi-tenancy with `tenant_id`, composite FKs, tenant-scoped uniqueness;
  RLS deferred (OQ-TEN-1).
- Timestamps in UTC `timestamptz`; ranges via `tstzrange`; per-location time zones (OQ-TIME-1).
- Direct search defaults: 14-day horizon, 15-min increments, 50 results (OQ-SCH-1).
- Weekly availability templates + blocks initially (OQ-AVL-1).
- Requirement quantity = 1 in the slice (OQ-SVC-1).

## Process

When an open question is answered: set it `RESOLVED`, record the decision in
`04-DECISION-REGISTER.md` (and an ADR if material), update the affected docs and
`PROJECT_CONTEXT.md`, and remove the now-obsolete assumption. If a conflict surfaces,
resolve via the precedence order in `PROJECT_CONTEXT.md` and record the resolution here.
