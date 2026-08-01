# ADR-009 — Runtime: Java 21 (LTS) and Spring Boot 3.x

- **Status:** ACCEPTED
- **Date:** 2026-08-01
- **Decision (register):** DEC-009
- **Related PRD:** PRD-NFR-009, PRD-NFR-001, PRD-NFR-002

## Context

The instruction fixes Java + Spring Boot and asks for a current supported Java version and
Cloud Run deployability. We must pin specific versions.

## Decision

- **Java 21 (LTS)** as the language/runtime.
- **Spring Boot 3.x** as the application framework.

Rationale:
- Java 21 is a current LTS with long support, records, sealed types, pattern matching and
  **virtual threads** (useful for I/O-bound request handling and horizontal scale,
  PRD-NFR-001).
- Spring Boot 3.x requires Java 17+ and pairs naturally with Java 21; provides Actuator
  (health/liveness/readiness, PRD-NFR-003), Spring Security (Firebase token filter,
  ADR-010), Spring Data (tenant-scoped repositories, ADR-002) and first-class container/
  Cloud Run support (PRD-NFR-002).

## Alternatives considered

1. **Java 17 (LTS)** — supported and valid, but Java 21 is the newer LTS with virtual
   threads and later language features; chosen for longevity.
2. **Latest non-LTS Java** — rejected: shorter support window; LTS preferred for a
   foundation.
3. **Non-Spring frameworks (Quarkus/Micronaut)** — out of scope: the instruction mandates
   Spring Boot.

## Consequences

- (+) Long-supported LTS; modern language features; virtual threads for scalable I/O.
- (+) Rich Spring ecosystem for auth, data, actuator, testing (Testcontainers integration).
- (−) Team/tooling must target Java 21 (CI images, base container).
- Revisit at future LTS boundaries; a change would supersede this ADR.
