# ADR-010 — Firebase Authentication with PostgreSQL Authorization

- **Status:** ACCEPTED
- **Date:** 2026-08-01
- **Decision (register):** DEC-010
- **Related PRD:** PRD-IDN-001..004, PRD-SEC-001..004, PRD-MEM-001..004

## Context

The instruction mandates Firebase Authentication for user identity. We must define how
Firebase identity maps to the application's authorization model without leaking Firebase
SDK types into the domain (ADR-001 boundary rules) and without trusting client-supplied
tenant ids (ADR-002).

## Decision

- **Firebase Authentication** establishes **global identity**. The backend **verifies
  Firebase ID tokens** (issuer, audience, signature, expiry) in a Spring Security filter;
  it never manages passwords (PRD-IDN-001, PRD-SEC-001).
- **PostgreSQL owns application authorization state**: `ApplicationUser` (linked 1:1 to a
  Firebase UID), `OrganisationMember`, active tenant, `Role`s and `Permission`s
  (PRD-IDN-002).
- **ApplicationUser ≠ person Resource** (PRD-IDN-003): a login and a schedulable staff
  member are independent.
- **Active tenant** is resolved from a request header/claim and **validated against
  membership** — never trusted from the client (PRD-IDN-004, PRD-SEC-002, ADR-002).
- **Authorization is permission-based** (PRD-MEM-003, PRD-SEC-003): permissions derived
  from the member's roles are checked at each use-case entry point; **no role-name string
  checks**. Denied actions are auditable (PRD-AUD-001).
- **Boundary discipline:** Firebase SDK types stay in the `identity` adapter; the domain
  (especially `scheduling`) never imports Firebase (ArchUnit-enforced, PRD-NFR-007).
- **Local dev:** a `local`-profile stub verifier or the Firebase Auth emulator (OQ-AUTH-1);
  never used in production.
- **Logging:** no tokens/secrets/PII in logs (PRD-SEC-004).

## Alternatives considered

1. **Firebase custom claims as the source of roles/permissions** — rejected as *source of
   truth*: authorization belongs in PostgreSQL where it is tenant-scoped, auditable and
   testable; claims can carry identity only. (Claims may cache active-tenant hints but are
   always re-validated.)
2. **Self-hosted auth (username/password, OAuth server)** — rejected: instruction mandates
   Firebase; also more security surface to own.
3. **Trusting a client `X-Tenant-Id` header without validation** — rejected: violates
   PRD-TEN-004/SEC-002.

## Consequences

- (+) Managed identity provider; standard token verification; clean identity/authorization
  split.
- (+) Tenant-scoped, auditable, testable authorization in the database.
- (+) Domain stays free of Firebase types (portable, testable engine).
- (−) Requires token-verification wiring, membership resolution and a local-dev auth story.
- (−) Two systems of record for "who" (Firebase identity) vs "what they may do" (PostgreSQL)
  — deliberate and clearly bounded.
