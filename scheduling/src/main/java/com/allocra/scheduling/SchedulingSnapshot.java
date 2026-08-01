package com.allocra.scheduling;

/**
 * Marker for the immutable input to the scheduling engine.
 *
 * <p>
 * A snapshot carries everything the engine needs to compute candidate options —
 * the service's requirements and constraints, the candidate resources with
 * their capabilities, availability and existing reservations in the requested
 * window — captured as immutable domain inputs. The engine reads a snapshot and
 * returns ranked candidates without touching any aggregate (PRD-SCH-001).
 *
 * <p>
 * Concrete snapshot fields are defined in the vertical slice (Deliverable C).
 * This interface exists now to anchor the engine's boundary and the
 * architecture tests.
 */
public interface SchedulingSnapshot {
}
