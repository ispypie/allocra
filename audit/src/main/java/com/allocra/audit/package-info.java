/**
 * Audit: AuditEvent for significant tenant-scoped actions (PRD-AUD-001).
 *
 * <p>
 * Module boundaries and allowed dependencies are documented in
 * docs/02-DOMAIN-MODEL.md and enforced by ArchitectureTest (PRD-NFR-007).
 * Domain code in this module must not depend on another module's adapter layer,
 * and must not leak infrastructure types into the scheduling engine.
 *
 * <p>
 * Concrete entities and services for this module are delivered in the vertical
 * slice (Deliverable C); this package currently establishes the module's home
 * and boundary.
 */
package com.allocra.audit;
