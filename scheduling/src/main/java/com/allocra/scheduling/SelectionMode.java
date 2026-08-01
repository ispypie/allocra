package com.allocra.scheduling;

/**
 * How a specific resource preference on a requirement is treated (PRD-SVC-005).
 *
 * <ul>
 * <li>{@link #REQUIRED} — a specific resource must be used (hard constraint);
 * <li>{@link #PREFERRED} — rank a specific resource higher (soft constraint);
 * <li>{@link #ANY} — any resource satisfying the capability requirement.
 * </ul>
 */
public enum SelectionMode {
	REQUIRED, PREFERRED, ANY
}
