# ADR-011 — API-First, Multiple UI Clients

- **Status:** ACCEPTED
- **Date:** 2026-08-02
- **Decision (register):** DEC-025, DEC-026
- **Related PRD:** PRD-NFR-010, PRD-NFR-011, PRD §12, ADR-005

## Context

The product needs more than one UI: existing Flutter admin and customer apps, plus a desire
to add **React** clients (and, later, a public self-service web UI). We must confirm the
backend supports multiple, independently-developed UI clients without per-client rework, and
identify what (if anything) must change to unblock a browser-based (React / Flutter-web)
client.

The backend is already a headless, stateless REST/JSON API: requests carry a Firebase ID
token (`Authorization: Bearer …`) and an `X-Tenant-Id` header; authorization is
permission-based; availability is returned as bookable **options, not calendars** (ADR-005);
errors are RFC-7807. None of this couples to a UI framework.

## Decision

**Adopt an explicit API-first posture: one client-agnostic, versioned REST/JSON API serves any
number of UI clients.**

- **Shared admin API.** The `/v1` admin API is shared by all internal clients (Flutter admin
  **and** React admin). We do **not** introduce a per-client backend-for-frontend (BFF); a
  single well-versioned API is simpler and sufficient. Revisit only if clients diverge enough
  to justify a BFF (record a new ADR if so).
- **Separate public API for self-service.** The future public/self-service channel is a
  **distinct API surface** (restricted fields, its own rate-limiting/abuse controls, no
  staff-schedule exposure), shared by public clients (Flutter customer **and** React
  customer). This is consistent with ADR-005.
- **CORS is configurable per environment** (`allocra.cors.allowed-origins`), enforced by a
  filter ahead of authentication so browser preflight works and is not rejected by auth
  (PRD-NFR-010). This is the one concrete change required to unblock a browser client;
  non-browser clients (Flutter mobile/desktop) are unaffected.
- **Publish an OpenAPI specification** (PRD-NFR-011) so each client generates a typed SDK —
  TypeScript for React, Dart for Flutter — keeping the contract in sync across clients.
- **UI clients live in their own codebases/repos.** The backend stays headless; adding React
  does not touch the modular monolith.

## Alternatives considered

1. **Backend-for-Frontend per client** — rejected for now: extra surface and duplication with
   no current need; the API is already client-neutral. Revisit per ADR if clients diverge.
2. **Couple the API to one client (e.g. Flutter-shaped payloads)** — rejected: would block
   React and violate the client-agnostic goal.
3. **GraphQL gateway** — rejected for now: REST/JSON + OpenAPI meets the need with less
   machinery; can be reconsidered if clients need highly divergent read shapes.

## Consequences

- (+) React, Flutter, native and other clients consume the same API with no backend forks.
- (+) CORS + OpenAPI make browser clients and typed SDKs first-class.
- (+) Clear split between shared admin API and a separate public API for self-service.
- (−) CORS allowed-origins must be maintained per environment (config, not code).
- (−) The OpenAPI spec must be kept accurate (it is generated from the controllers, which
  mitigates drift).
- Auth is unchanged and already multi-client: Firebase Auth has SDKs for web (React) and
  Flutter; both mint the same ID token the backend verifies (real verifier still pending,
  DEC-020).
