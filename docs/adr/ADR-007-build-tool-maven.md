# ADR-007 — Build Tool: Maven

- **Status:** ACCEPTED
- **Date:** 2026-08-01
- **Decision (register):** DEC-007, DEC-012
- **Related PRD:** PRD-NFR-008, PRD-NFR-007

## Context

The instruction requires choosing and recording Maven or Gradle. The repository is a
modular monolith intended to be continued by other humans **and** AI agents, so
predictability and ubiquity matter as much as raw build ergonomics.

## Decision

Use **Maven** with a **multi-module reactor** (root `pom.xml`, one module per bounded
context: `common`, `identity`, `tenancy`, `membership`, `resources`, `services`,
`availability`, `scheduling`, `bookings`, `reservations`, `audit`, plus `app`).

- Module boundaries are also enforced by **ArchUnit** (DEC-012, PRD-NFR-007), not by build
  configuration alone.
- Dependency and plugin versions centralised via `dependencyManagement`/`pluginManagement`
  (and Spring Boot BOM).
- Plugins for formatting (Spotless) and static analysis wired uniformly across modules.

*User selected Maven when offered Gradle vs Maven during Milestone 1 planning.*

## Alternatives considered

1. **Gradle (Kotlin DSL)** — strong multi-module ergonomics, faster incremental builds,
   version catalogs. Not chosen: Maven's ubiquity and declarative predictability are more
   valuable for a codebase meant to be picked up by arbitrary human/AI contributors, and
   the multi-module + ArchUnit approach meets our boundary-enforcement needs.

## Consequences

- (+) Familiar, declarative, widely understood by tooling and agents.
- (+) Mature multi-module support; easy Spring Boot integration via BOM.
- (−) More verbose than Gradle; slower clean builds on large trees.
- (−) Cross-module build logic is less flexible than Gradle convention plugins (acceptable
  at this scale).
- Reversible: migrating to Gradle later is possible but would supersede this ADR.
