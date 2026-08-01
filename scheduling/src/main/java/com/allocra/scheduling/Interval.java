package com.allocra.scheduling;

import java.time.Instant;
import java.util.Objects;

/**
 * A half-open time interval {@code [start, end)} in UTC (OQ-TIME-1). Used for
 * slots, availability windows, blocks and reservation windows in the pure
 * engine.
 *
 * @param start
 *            inclusive start; never {@code null}
 * @param end
 *            exclusive end; never {@code null} and must be after {@code start}
 */
public record Interval(Instant start, Instant end) {

	public Interval {
		Objects.requireNonNull(start, "start");
		Objects.requireNonNull(end, "end");
		if (!end.isAfter(start)) {
			throw new IllegalArgumentException("end (" + end + ") must be after start (" + start + ")");
		}
	}

	/** Two intervals overlap if they share any instant (half-open semantics). */
	public boolean overlaps(Interval other) {
		return start.isBefore(other.end) && other.start.isBefore(end);
	}

	/** True if this interval fully contains {@code inner}. */
	public boolean contains(Interval inner) {
		return !start.isAfter(inner.start) && !end.isBefore(inner.end);
	}
}
