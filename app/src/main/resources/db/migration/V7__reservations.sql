-- Reservations enforce exclusive consumption in PostgreSQL (ADR-004, PRD-RSV-*).
-- start_at/end_at are the writable columns; `during` is a generated tstzrange used only by
-- the exclusion constraint, so the application never writes a range type directly.

CREATE TABLE reservation (
    tenant_id   uuid NOT NULL,
    id          uuid NOT NULL,
    booking_id  uuid NOT NULL,
    resource_id uuid NOT NULL,
    start_at    timestamptz NOT NULL,
    end_at      timestamptz NOT NULL,
    during      tstzrange GENERATED ALWAYS AS (tstzrange(start_at, end_at)) STORED,  -- '[)' bounds
    status      text NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'RELEASED')),
    PRIMARY KEY (tenant_id, id),
    FOREIGN KEY (tenant_id, booking_id) REFERENCES booking (tenant_id, id),
    FOREIGN KEY (tenant_id, resource_id) REFERENCES resource (tenant_id, id),
    CONSTRAINT reservation_time_order CHECK (end_at > start_at)
);

-- No two ACTIVE reservations for the same resource within a tenant may overlap in time.
-- Concurrent confirmations racing for the same resource → only one succeeds; the other
-- raises exclusion_violation (SQLSTATE 23P01), mapped to HTTP 409 (PRD-RSV-004, PRD-BKG-006).
ALTER TABLE reservation
    ADD CONSTRAINT reservation_no_overlap
    EXCLUDE USING gist (
        tenant_id   WITH =,
        resource_id WITH =,
        during      WITH &&
    ) WHERE (status = 'ACTIVE');
