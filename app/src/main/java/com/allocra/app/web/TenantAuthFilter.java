package com.allocra.app.web;

import com.allocra.app.persistence.AuthRepository;
import com.allocra.common.tenant.TenantContext;
import com.allocra.common.tenant.TenantId;
import com.allocra.identity.TokenVerifier;
import com.allocra.membership.Membership;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates every non-actuator request and resolves the active tenant
 * (PRD-SEC-001/002).
 *
 * <p>
 * The bearer token is verified (identity), the {@code X-Tenant-Id} header names
 * the requested tenant, and the caller's ACTIVE membership of that tenant is
 * looked up in PostgreSQL. The client-supplied tenant id is
 * <strong>never</strong> trusted on its own — a caller with no membership in
 * that tenant is rejected with 403 (PRD-TEN-004). On success the tenant is
 * bound to {@link TenantContext} and the {@link Membership} is exposed as a
 * request attribute.
 */
@Component
public class TenantAuthFilter extends OncePerRequestFilter {

	static final String MEMBERSHIP_ATTRIBUTE = "allocra.membership";
	private static final String TENANT_HEADER = "X-Tenant-Id";

	private final TokenVerifier tokenVerifier;
	private final AuthRepository authRepository;

	public TenantAuthFilter(TokenVerifier tokenVerifier, AuthRepository authRepository) {
		this.tokenVerifier = tokenVerifier;
		this.authRepository = authRepository;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return request.getRequestURI().startsWith("/actuator");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith("Bearer ")) {
			writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "missing or malformed Authorization header");
			return;
		}
		TokenVerifier.VerifiedIdentity identity;
		try {
			identity = tokenVerifier.verify(header.substring("Bearer ".length()));
		} catch (TokenVerifier.InvalidTokenException e) {
			writeError(response, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
			return;
		}

		String tenantHeader = request.getHeader(TENANT_HEADER);
		if (tenantHeader == null || tenantHeader.isBlank()) {
			writeError(response, HttpServletResponse.SC_BAD_REQUEST, "missing " + TENANT_HEADER + " header");
			return;
		}
		TenantId tenantId;
		try {
			tenantId = TenantId.of(tenantHeader.trim());
		} catch (IllegalArgumentException e) {
			writeError(response, HttpServletResponse.SC_BAD_REQUEST, "invalid " + TENANT_HEADER + " header");
			return;
		}

		Optional<Membership> membership = authRepository.findActiveMembership(tenantId, identity.firebaseUid());
		if (membership.isEmpty()) {
			// Authenticated, but not a member of the requested tenant → never trust the id
			// (PRD-TEN-004).
			writeError(response, HttpServletResponse.SC_FORBIDDEN, "not a member of the requested tenant");
			return;
		}

		try {
			TenantContext.set(tenantId);
			request.setAttribute(MEMBERSHIP_ATTRIBUTE, membership.get());
			chain.doFilter(request, response);
		} finally {
			TenantContext.clear();
		}
	}

	private static void writeError(HttpServletResponse response, int status, String detail) throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write("{\"status\":" + status + ",\"detail\":\"" + escape(detail) + "\"}");
	}

	private static String escape(String s) {
		return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
