package com.allocra.common.error;

/**
 * Thrown when the caller is authenticated but lacks the permission required for
 * an action (PRD-SEC-003) or is not a member of the requested tenant
 * (PRD-TEN-004). The web layer maps this to HTTP 403.
 */
public class NotAuthorizedException extends RuntimeException {
	public NotAuthorizedException(String message) {
		super(message);
	}
}
