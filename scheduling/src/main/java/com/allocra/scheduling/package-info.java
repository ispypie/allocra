/**
 * The pure scheduling engine.
 *
 * <p>
 * This package computes candidate options in memory from an immutable
 * {@link com.allocra.scheduling.SchedulingSnapshot} and mutates nothing. It
 * must not hold authoritative calendar state and must not depend on HTTP,
 * Spring MVC, JPA/Hibernate entities or the Firebase SDK (ADR-001, PRD-SCH-001,
 * PRD-NFR-001). These constraints are enforced by {@code ArchitectureTest}
 * (PRD-NFR-007).
 *
 * <p>
 * The concrete candidate-search and constraint-evaluation implementation is
 * delivered in the thin vertical slice (Deliverable C); this package currently
 * defines the engine's input contract only.
 */
package com.allocra.scheduling;
