-- Generic resource model (ADR-001, PRD-RES-*). Every schedulable item is a `resource`
-- of a `resource_type` carrying a controlled base_kind. Composite FKs include tenant_id.

CREATE TABLE location (
    tenant_id uuid NOT NULL REFERENCES tenant (id),
    id        uuid NOT NULL,
    name      text NOT NULL,
    PRIMARY KEY (tenant_id, id)
);

CREATE TABLE resource_type (
    tenant_id uuid NOT NULL REFERENCES tenant (id),
    id        uuid NOT NULL,
    code      text NOT NULL,
    base_kind text NOT NULL CHECK (base_kind IN ('PERSON', 'PLACE', 'ASSET')),
    PRIMARY KEY (tenant_id, id),
    CONSTRAINT resource_type_code_unique UNIQUE (tenant_id, code)
);

CREATE TABLE resource (
    tenant_id        uuid NOT NULL REFERENCES tenant (id),
    id               uuid NOT NULL,
    resource_type_id uuid NOT NULL,
    name             text NOT NULL,
    location_id      uuid,
    mobility         text CHECK (mobility IN ('FIXED', 'MOVABLE', 'POOLED')),
    active           boolean NOT NULL DEFAULT true,
    PRIMARY KEY (tenant_id, id),
    FOREIGN KEY (tenant_id, resource_type_id) REFERENCES resource_type (tenant_id, id),
    FOREIGN KEY (tenant_id, location_id) REFERENCES location (tenant_id, id)
);

CREATE TABLE resource_capability (
    tenant_id       uuid NOT NULL,
    id              uuid NOT NULL,
    resource_id     uuid NOT NULL,
    capability_type text NOT NULL,
    level           int,
    valid_from      date,
    valid_to        date,
    PRIMARY KEY (tenant_id, id),
    FOREIGN KEY (tenant_id, resource_id) REFERENCES resource (tenant_id, id)
);

-- Which resources may be combined (PRD-RES-008); e.g. FIXED equipment lists its room(s).
CREATE TABLE resource_compatibility (
    tenant_id              uuid NOT NULL,
    resource_id            uuid NOT NULL,
    compatible_resource_id uuid NOT NULL,
    PRIMARY KEY (tenant_id, resource_id, compatible_resource_id),
    FOREIGN KEY (tenant_id, resource_id) REFERENCES resource (tenant_id, id),
    FOREIGN KEY (tenant_id, compatible_resource_id) REFERENCES resource (tenant_id, id)
);
