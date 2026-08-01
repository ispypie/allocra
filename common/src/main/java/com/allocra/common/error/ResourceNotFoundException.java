package com.allocra.common.error;

/**
 * Thrown when a tenant-scoped entity does not exist. The web layer maps this to
 * HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {
	public ResourceNotFoundException(String message) {
		super(message);
	}
}
