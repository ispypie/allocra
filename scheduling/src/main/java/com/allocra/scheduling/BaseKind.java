package com.allocra.scheduling;

/**
 * The controlled base kind of a schedulable resource (ADR-001, PRD-RES-002).
 * The scheduling engine reasons only in terms of this generic kind, never
 * concrete types like "staff" or "room", so new resource types require no
 * engine change (PRD-RES-003).
 */
public enum BaseKind {
	PERSON, PLACE, ASSET
}
