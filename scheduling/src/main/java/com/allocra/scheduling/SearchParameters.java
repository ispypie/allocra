package com.allocra.scheduling;

import java.time.Duration;
import java.util.Objects;

/**
 * Tuning parameters for direct availability search (PRD-SCH-005). Kept
 * deliberately simple: fixed increments and a capped result count. The search
 * horizon is expressed by the request window on the snapshot (the application
 * layer clamps it to the default horizon; OQ-SCH-1).
 *
 * @param increment
 *            the step between candidate start times (default 15 minutes)
 * @param maxResults
 *            the maximum number of ranked options to return (default 50)
 */
public record SearchParameters(Duration increment, int maxResults) {

	public SearchParameters {
		Objects.requireNonNull(increment, "increment");
		if (increment.isZero() || increment.isNegative()) {
			throw new IllegalArgumentException("increment must be positive");
		}
		if (maxResults <= 0) {
			throw new IllegalArgumentException("maxResults must be positive");
		}
	}

	/**
	 * Default parameters per OQ-SCH-1: 15-minute increments, at most 50 options.
	 */
	public static SearchParameters defaults() {
		return new SearchParameters(Duration.ofMinutes(15), 50);
	}
}
