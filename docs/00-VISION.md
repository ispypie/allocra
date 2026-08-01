# 00 — Vision

## North star

A scheduling platform where **any organisation** can describe **what a service needs**
— people, places, assets, with the right qualifications and compatibilities — and the
system reliably allocates a valid combination at a valid time, for one organisation
today and for the public tomorrow, **without re-modelling the core**.

## Problem statement

Real-world booking is rarely "book a person" or "book a room". It is "book a *service*
that simultaneously requires a qualified person **and** a compatible place **and** the
right equipment, none of which may double-book, some of which must move or be set up
between uses, and any of which might be swapped for an equivalent later." Most scheduling
tools hardcode one resource shape (usually staff *or* rooms), so every new resource type
or constraint becomes a schema change and a scheduling-engine change.

Allocra treats **every schedulable thing as a generic `Resource`** and expresses services
as **requirements over capabilities**, so the scheduling engine is written once and new
resource types are configuration, not code.

## Who it is for

- **Organisations** delivering appointment- or session-based services that combine
  staff, spaces and equipment: clinics, inspection centres, training providers,
  maintenance operations, and similar.
- **Internal schedulers/receptionists** first; **self-service customers** next.

## What "good" looks like

1. Adding a new resource type (e.g. *vehicle*, *inspection bay*, *treatment station*)
   requires **no change to the scheduling engine**.
2. A booking's identity is stable even when the resources assigned to it change.
3. Two people cannot confirm bookings that consume the same exclusive resource at the
   same time — **guaranteed by the database**, not by hopeful application logic.
4. One organisation can **never** see or reference another organisation's data.
5. A product or architecture decision made months ago is discoverable in the repository,
   with its reasoning, alternatives and consequences — not reconstructed from chat logs.
6. The internal product ships without any assumption that would have to be unwound to
   add public self-service booking.

## Strategic sequencing

1. **Internal booking** (this and the next milestones) — authenticated members create
   bookings; direct availability search; atomic confirmation; multi-tenant isolation.
2. **Self-service readiness carried throughout** — neutral `BookingSubject`, booking
   `channel`, no actor==subject==provider assumptions, options-not-calendars API shape.
3. **Public self-service** (future) — published services, public availability, holds,
   policies, notifications, optional approval.
4. **Advanced scheduling** (future, ADR-gated) — schedule repair (proposal-only,
   constrained), capacity/pooled resources, richer qualification rules.

## Guiding principles

- **Generic over hardcoded** — one resource model, one scheduling engine.
- **Explicit over dynamic** — model flexibility as named domain concepts, not a
  configurable rules platform.
- **Safe defaults over open-ended power** — opinionated, predictable behaviour first.
- **Database as authority** — correctness of exclusive consumption enforced in PostgreSQL.
- **Traceable intent** — requirements, decisions, implementation and tests stay aligned.

## Non-goals

Allocra is **not** a CRM, not a payments platform, and not a whole-schedule optimiser.
It does not aim to model every conceivable constraint via a generic rules engine. See
`docs/01-PRD.md` §Non-goals and `PROJECT_CONTEXT.md` §Explicit non-goals.
