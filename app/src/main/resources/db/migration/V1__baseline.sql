-- V1 baseline (ADR-008: Flyway plain SQL owns the schema).
--
-- Enables btree_gist, which is required for the reservation non-overlap exclusion
-- constraint added with the reservations schema in the vertical slice (ADR-004,
-- PRD-RSV-004). Creates the tenant table: the root of all tenant-owned data (ADR-002).

CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE tenant (
    id           uuid PRIMARY KEY,
    -- Public tenant identity/slug; supports the future self-service channel (ADR-005).
    slug         text NOT NULL,
    display_name text NOT NULL,
    created_at   timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT tenant_slug_unique UNIQUE (slug)
);

COMMENT ON TABLE tenant IS 'An organisation. Root discriminator (tenant_id) for all tenant-owned data (PRD-TEN-001/002).';
