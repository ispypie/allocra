package com.allocra.app.web;

import com.allocra.identity.TokenVerifier;
import org.springframework.stereotype.Component;

/**
 * Local/test token verifier (DEC-020): treats the bearer token as the caller's
 * Firebase UID. This keeps the whole slice runnable locally and in tests
 * without a Firebase project or emulator. A real Firebase ID-token verifier
 * (ADR-010) must replace this before production — it is the only
 * {@link TokenVerifier} wired today, so production would have no verifier until
 * that adapter is added.
 */
@Component
public class StubTokenVerifier implements TokenVerifier {

	@Override
	public VerifiedIdentity verify(String token) {
		if (token == null || token.isBlank()) {
			throw new InvalidTokenException("missing bearer token");
		}
		String firebaseUid = token.trim();
		return new VerifiedIdentity(firebaseUid, null, firebaseUid);
	}
}
