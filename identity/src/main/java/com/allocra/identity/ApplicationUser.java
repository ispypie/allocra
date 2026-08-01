package com.allocra.identity;

import java.util.Objects;
import java.util.UUID;

/**
 * A system login (PRD-IDN-002), linked 1:1 to a Firebase UID and global across
 * tenants. This is distinct from a schedulable person resource (PRD-IDN-003).
 *
 * @param id
 *            the application-user id
 * @param firebaseUid
 *            the Firebase UID establishing global identity
 * @param email
 *            optional email
 * @param displayName
 *            display name
 */
public record ApplicationUser(UUID id, String firebaseUid, String email, String displayName) {

	public ApplicationUser {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(firebaseUid, "firebaseUid");
		Objects.requireNonNull(displayName, "displayName");
	}
}
