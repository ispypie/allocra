-- Services and their requirements (PRD-SVC-*). A requirement describes WHAT is needed
-- (kind + capability), not a specific named resource unless the request requires one.

CREATE TABLE service_type (
    tenant_id        uuid NOT NULL REFERENCES tenant (id),
    id               uuid NOT NULL,
    code             text NOT NULL,
    name             text NOT NULL,
    duration_minutes int NOT NULL CHECK (duration_minutes > 0),
    PRIMARY KEY (tenant_id, id),
    CONSTRAINT service_type_code_unique UNIQUE (tenant_id, code)
);

CREATE TABLE resource_requirement (
    tenant_id                uuid NOT NULL,
    id                       uuid NOT NULL,
    service_type_id          uuid NOT NULL,
    base_kind                text NOT NULL CHECK (base_kind IN ('PERSON', 'PLACE', 'ASSET')),
    required                 boolean NOT NULL DEFAULT true,
    selection_mode           text NOT NULL DEFAULT 'ANY'
                                 CHECK (selection_mode IN ('REQUIRED', 'PREFERRED', 'ANY')),
    required_capability_type text,
    min_level                int,
    -- Slice supports exactly one resource per requirement (OQ-SVC-1).
    quantity                 int NOT NULL DEFAULT 1 CHECK (quantity = 1),
    PRIMARY KEY (tenant_id, id),
    FOREIGN KEY (tenant_id, service_type_id) REFERENCES service_type (tenant_id, id)
);
