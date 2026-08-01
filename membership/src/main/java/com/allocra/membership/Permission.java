package com.allocra.membership;

/**
 * Fine-grained permissions checked at action time (PRD-MEM-003, PRD-SEC-003).
 * Authorization is always by permission, never by role name.
 */
public enum Permission {
	RESOURCE_MANAGE, SERVICE_MANAGE, AVAILABILITY_MANAGE, BOOKING_CREATE, BOOKING_VIEW, BOOKING_CANCEL, BOOKING_RESCHEDULE, BOOKING_UPDATE, MEMBERSHIP_MANAGE
}
