package com.allocra.common.tenant;

import java.util.Objects;
import java.util.Optional;

/**
 * Holds the active {@link TenantId} for the current request/worker thread.
 *
 * <p>
 * The active tenant is resolved from the authenticated user's validated
 * membership and is <strong>never</strong> trusted from a raw client value
 * (PRD-TEN-004 / PRD-SEC-002). This context is per-request scratch state only —
 * it is <em>not</em> authoritative calendar state and does not violate the
 * statelessness requirement (PRD-NFR-001 / PRD-SCH-001).
 *
 * <p>
 * Callers must {@link #clear()} at the end of the request (e.g. in a servlet
 * filter's {@code finally} block) to avoid leaking a tenant onto a pooled
 * thread.
 */
public final class TenantContext {

	private static final ThreadLocal<TenantId> CURRENT = new ThreadLocal<>();

	private TenantContext() {
	}

	public static void set(TenantId tenantId) {
		CURRENT.set(Objects.requireNonNull(tenantId, "tenantId must not be null"));
	}

	public static Optional<TenantId> current() {
		return Optional.ofNullable(CURRENT.get());
	}

	/**
	 * @return the active tenant
	 * @throws IllegalStateException
	 *             if no tenant is bound to the current thread
	 */
	public static TenantId require() {
		TenantId tenantId = CURRENT.get();
		if (tenantId == null) {
			throw new IllegalStateException("No active tenant bound to the current thread");
		}
		return tenantId;
	}

	public static void clear() {
		CURRENT.remove();
	}
}
