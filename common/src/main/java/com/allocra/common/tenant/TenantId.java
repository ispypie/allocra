package com.allocra.common.tenant;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed identifier for a {@code Tenant} (organisation).
 *
 * <p>
 * Using a value type instead of a raw {@link UUID}/{@link String} makes tenant
 * scoping explicit at every boundary and prevents accidentally passing, say, a
 * {@code ResourceId} where a {@code TenantId} is expected (see PRD-TEN-003,
 * ADR-002).
 *
 * @param value
 *            the underlying identifier; never {@code null}
 */
public record TenantId(UUID value) {

	public TenantId {
		Objects.requireNonNull(value, "TenantId value must not be null");
	}

	public static TenantId of(UUID value) {
		return new TenantId(value);
	}

	public static TenantId of(String value) {
		return new TenantId(UUID.fromString(value));
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
