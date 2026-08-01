package com.allocra.identity;

/**
 * Port for verifying an authentication token and extracting the caller's global
 * identity (ADR-010). The real implementation verifies a Firebase ID token; a
 * local stub is used for tests and local development (DEC-020). Keeping this a
 * plain interface in the domain means the Firebase SDK never leaks into the
 * domain (enforced by ArchitectureTest, PRD-NFR-007).
 */
public interface TokenVerifier {

	/**
	 * @param token
	 *            the raw bearer token
	 * @return the verified identity
	 * @throws InvalidTokenException
	 *             if the token is missing, malformed or not valid
	 */
	VerifiedIdentity verify(String token);

	/** The caller's verified global identity. */
	record VerifiedIdentity(String firebaseUid, String email, String displayName) {
	}

	/**
	 * Thrown when a token cannot be verified; the web layer maps this to HTTP 401.
	 */
	class InvalidTokenException extends RuntimeException {
		public InvalidTokenException(String message) {
			super(message);
		}
	}
}
