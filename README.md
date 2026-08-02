# Allocra

Multi-tenant resource scheduling platform — organisations define bookable **services** and
the platform allocates the required combination of **people, places and assets**.

> **New here? Read [`PROJECT_CONTEXT.md`](PROJECT_CONTEXT.md) first.** It is mandatory
> orientation for every human and AI contributor and indexes all other documentation.

## Status

Milestone 1 — foundation. Deliverable A (documentation) and Deliverable B (engineering
foundation) are in place. The thin internal-booking vertical slice (Deliverable C) is
**planned, not yet implemented**. See `docs/01-PRD.md` and `PROJECT_CONTEXT.md`.

## Tech stack

Java 21 · Spring Boot 3.4 · Maven (multi-module) · PostgreSQL · Flyway · Firebase
Authentication · Testcontainers · Google Cloud Run. Rationale in `docs/adr/`.

## Prerequisites

- JDK 21+ (the build targets Java 21 via `--release 21`; a newer JDK works for building).
- Docker (only for the Testcontainers integration tests / `verify`).
- PostgreSQL for running the app locally (or use your own container).

## Build & test

```bash
./mvnw package               # compile + unit, architecture and documentation tests (no Docker)
./mvnw verify                # the above + Testcontainers integration tests (requires Docker)
./mvnw -Pquality verify      # additionally runs SpotBugs static analysis (used by CI)
./mvnw spotless:apply        # auto-format the codebase (palantir-java-format)
```

- Unit / architecture / documentation tests are named `*Test` (surefire).
- Integration tests are named `*IT` (failsafe) and use real PostgreSQL via Testcontainers.

## Try the demo (no Docker)

```bash
./demo/embedded/run.sh
```
Runs the whole backend on an embedded PostgreSQL with seeded data, then prints a cheat sheet.
Explore it at http://localhost:8080/swagger-ui. See [`demo/`](demo/) for details.

## Run locally

```bash
# Point at a local PostgreSQL (defaults shown; override via env vars)
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/allocra
export SPRING_DATASOURCE_USERNAME=allocra
export SPRING_DATASOURCE_PASSWORD=allocra
export SPRING_PROFILES_ACTIVE=local
./mvnw -pl app spring-boot:run
```

Health probes: `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`.

## Container / Cloud Run

```bash
docker build -t allocra:local .
docker run -p 8080:8080 -e PORT=8080 \
  -e SPRING_DATASOURCE_URL=... -e SPRING_DATASOURCE_USERNAME=... -e SPRING_DATASOURCE_PASSWORD=... \
  allocra:local
```

The container runs with the `cloud` profile (structured ECS logging) and binds to `PORT`.

## Modules

`common` · `identity` · `tenancy` · `membership` · `resources` · `services` ·
`availability` · `scheduling` (pure engine) · `bookings` · `reservations` · `audit` ·
`app` (bootstrap). Boundaries are enforced by ArchUnit
(`app/src/test/.../ArchitectureTest.java`). See `docs/02-DOMAIN-MODEL.md`.

## Documentation

Everything lives in [`docs/`](docs/): vision, PRD (with stable requirement ids), domain
model, technical specification, decision register, glossary, future ideas, open questions,
requirements traceability, and ADRs (`docs/adr/`).

**Contributor rule:** before a material change, read `PROJECT_CONTEXT.md`, the relevant PRD
sections, technical specification and accepted ADRs; after the change, update the affected
documentation, requirements traceability and decision records (see the Definition of Done
in `docs/03-TECHNICAL-SPECIFICATION.md`).
