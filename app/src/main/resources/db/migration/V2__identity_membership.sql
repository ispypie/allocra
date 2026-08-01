-- Identity & membership (ADR-002, ADR-010).
-- ApplicationUser is GLOBAL (no tenant_id): a login linked 1:1 to a Firebase UID.
-- Membership, roles and permissions are tenant-owned.

CREATE TABLE application_user (
    id           uuid PRIMARY KEY,
    firebase_uid text NOT NULL,
    email        text,
    display_name text NOT NULL,
    created_at   timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT application_user_firebase_uid_unique UNIQUE (firebase_uid)
);

CREATE TABLE organisation_member (
    tenant_id  uuid NOT NULL REFERENCES tenant (id),
    id         uuid NOT NULL,
    user_id    uuid NOT NULL REFERENCES application_user (id),
    status     text NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (tenant_id, id),
    -- A user has at most one membership per tenant (tenant-scoped uniqueness, PRD-TEN-005).
    CONSTRAINT organisation_member_user_unique UNIQUE (tenant_id, user_id)
);

CREATE TABLE organisation_member_role (
    tenant_id uuid NOT NULL,
    member_id uuid NOT NULL,
    role      text NOT NULL,
    PRIMARY KEY (tenant_id, member_id, role),
    -- Composite FK carries tenant_id so a role cannot reference another tenant's member (PRD-TEN-006).
    FOREIGN KEY (tenant_id, member_id) REFERENCES organisation_member (tenant_id, id)
);
