package com.allocra.bookings;

/**
 * How an assignment may change after confirmation (PRD-ASN-003). The slice
 * creates {@link #REASSIGNABLE} assignments; locking behaviour is modelled but
 * deferred.
 */
public enum AssignmentPolicy {
	REASSIGNABLE, LOCKED, LOCKED_BY_POLICY
}
