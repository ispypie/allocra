# ADR-005 — Internal-First, Self-Service-Ready

- **Status:** ACCEPTED
- **Date:** 2026-08-01
- **Decision (register):** DEC-005, DEC-016
- **Related PRD:** PRD-BKG-003, PRD-SUB-001..003, PRD §12, PRD-IDN-003

## Context

The initial delivery is authenticated internal booking. A public self-service channel is
expected soon. We must ship internal booking now **without** baking in assumptions that a
public channel would force us to unwind.

## Decision

Build **internal booking only**, but keep the core model self-service-ready:

- Every booking records a **`BookingChannel`** ∈ {INTERNAL, SELF_SERVICE, API, IMPORT};
  only INTERNAL is delivered now (PRD-BKG-003).
- Use a neutral **`BookingSubject`** (person/organisation/asset receiving the service) —
  never `Customer` in the core domain, and not a CRM (PRD-SUB-001/002).
- Keep four roles distinct: **authenticated actor**, **organisation member**,
  **booking subject**, **assigned staff resource**. Never assume actor == subject ==
  provider (PRD-SUB-003, PRD-IDN-003).
- The read API exposes **bookable options, not internal resource calendars**, so a public
  variant can reuse the shape without leaking staff schedules.
- Do **not** assume all bookings are internal, all services public, that public users see
  staff schedules, or that all bookings are immediately confirmed.

**Explicitly deferred** (needed by self-service, built later): booking **holds**
(PRD-BKG-007 / DEC-016), published services + public tenant slug, public availability
search, restricted resource-detail exposure, cancellation/rescheduling policies,
notifications, optional approval, optional customer resource selection. **No public UI now.**

Deployment target is **Google Cloud Run** (stateless container); Firebase may provide
hosting/platform for future front-ends.

## Alternatives considered

1. **Build internal only with internal-specific shortcuts** (resource ids on booking,
   `Customer` entity, actor==subject) — rejected: cheap now, expensive redesign later;
   contradicts ADR-003.
2. **Build public self-service now** — rejected: out of scope, larger surface, security and
   policy work not yet needed.

## Consequences

- (+) Public channel becomes additive, not a redesign.
- (+) Clear separation of actor/member/subject/provider avoids conflation bugs.
- (−) Slightly more modelling now (channel, neutral subject, options-shaped API) for
  future payoff.
- (−) Requires ongoing review that new code respects self-service-ready assumptions
  (checked per change against this ADR).
