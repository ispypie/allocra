-- Location-level business hours & closures, timezone-aware (PRD-AVL-005/006/007, DEC-028).
-- These constrain bookability in addition to each resource's own availability; they are
-- folded into effective availability at snapshot-build time, so the scheduling engine is
-- unchanged. Times are interpreted in the location's timezone (DST-aware).

ALTER TABLE location ADD COLUMN timezone text NOT NULL DEFAULT 'UTC';

CREATE TABLE location_operating_hours (
    tenant_id   uuid NOT NULL,
    id          uuid NOT NULL,
    location_id uuid NOT NULL,
    day_of_week int NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),  -- ISO-8601: 1=Mon..7=Sun
    open_time   time NOT NULL,
    close_time  time NOT NULL,
    PRIMARY KEY (tenant_id, id),
    FOREIGN KEY (tenant_id, location_id) REFERENCES location (tenant_id, id),
    CONSTRAINT location_operating_hours_order CHECK (close_time > open_time)
);

-- Full-day closures (inclusive date range), e.g. public holidays / one-off closed days.
CREATE TABLE location_closure (
    tenant_id   uuid NOT NULL,
    id          uuid NOT NULL,
    location_id uuid NOT NULL,
    start_date  date NOT NULL,
    end_date    date NOT NULL,
    reason      text,
    PRIMARY KEY (tenant_id, id),
    FOREIGN KEY (tenant_id, location_id) REFERENCES location (tenant_id, id),
    CONSTRAINT location_closure_order CHECK (end_date >= start_date)
);
