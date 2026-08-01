package com.allocra.common.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TenantContextTest {

	@AfterEach
	void tearDown() {
		TenantContext.clear();
	}

	@Test
	@DisplayName("PRD-TEN-004: require() fails when no tenant is bound to the thread")
	void requireFailsWhenNoTenantBound() {
		assertThrows(IllegalStateException.class, TenantContext::require);
		assertTrue(TenantContext.current().isEmpty());
	}

	@Test
	@DisplayName("PRD-TEN-003: the bound tenant is returned and cleared correctly")
	void boundTenantIsReturnedAndCleared() {
		TenantId tenant = TenantId.of(UUID.randomUUID());
		TenantContext.set(tenant);

		assertEquals(tenant, TenantContext.require());
		assertEquals(tenant, TenantContext.current().orElseThrow());

		TenantContext.clear();
		assertTrue(TenantContext.current().isEmpty());
	}
}
