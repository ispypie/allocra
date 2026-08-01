-- Availability: weekly rule templates + explicit blocks (OQ-AVL-1, PRD-AVL-*).
-- Blocks use plain timestamptz bounds (no range type needed here — only reservations
-- require the exclusion constraint, see V7).

CREATE TABLE availability_rule (
    tenant_id   uuid NOT NULL,
    id          uuid NOT NULL,
    resource_id uuid NOT NULL,
    day_of_week int NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),  -- ISO-8601: 1=Mon..7=Sun
    start_time  time NOT NULL,
    end_time    time NOT NULL,
    PRIMARY KEY (tenant_id, id),
    FOREIGN KEY (tenant_id, resource_id) REFERENCES resource (tenant_id, id),
    CONSTRAINT availability_rule_time_order CHECK (end_time > start_time)
);

CREATE TABLE blocked_availability (
    tenant_id   uuid NOT NULL,
    id          uuid NOT NULL,
    resource_id uuid NOT NULL,
    start_at    timestamptz NOT NULL,
    end_at      timestamptz NOT NULL,
    PRIMARY KEY (tenant_id, id),
    FOREIGN KEY (tenant_id, resource_id) REFERENCES resource (tenant_id, id),
    CONSTRAINT blocked_availability_time_order CHECK (end_at > start_at)
);
