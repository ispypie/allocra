/**
 * Shared kernel for Allocra: typed identifiers, tenant context, value types and
 * the error model reused across modules.
 *
 * <p>
 * This module must remain free of framework dependencies (no Spring, JPA, HTTP
 * or Firebase). Keeping it pure lets the scheduling domain and every other
 * module depend on it without inheriting infrastructure (see
 * docs/03-TECHNICAL-SPECIFICATION.md §2.3).
 */
package com.allocra.common;
