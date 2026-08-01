-- Audit trail for significant tenant-scoped actions (PRD-AUD-001).

CREATE TABLE audit_event (
    tenant_id   uuid NOT NULL REFERENCES tenant (id),
    id          uuid NOT NULL,
    actor       text NOT NULL,
    action      text NOT NULL,
    target_type text,
    target_id   uuid,
    at          timestamptz NOT NULL DEFAULT now(),
    detail      jsonb,
    PRIMARY KEY (tenant_id, id)
);
