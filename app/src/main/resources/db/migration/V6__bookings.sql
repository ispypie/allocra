-- Bookings and assignments (ADR-003). A booking has NO resource ids (PRD-BKG-001);
-- the resources satisfying it are recorded as separate resource_assignment rows, so an
-- assignment can change without changing the booking's identity (PRD-ASN-002).
-- BookingSubject is embedded as neutral value columns (PRD-SUB-*, not a CRM).

CREATE TABLE booking (
    tenant_id            uuid NOT NULL REFERENCES tenant (id),
    id                   uuid NOT NULL,
    service_type_id      uuid NOT NULL,
    subject_type         text NOT NULL,
    subject_display_name text NOT NULL,
    subject_email        text,
    subject_phone        text,
    subject_external_ref text,
    start_at             timestamptz NOT NULL,
    end_at               timestamptz NOT NULL,
    status               text NOT NULL
                             CHECK (status IN ('CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW')),
    channel              text NOT NULL
                             CHECK (channel IN ('INTERNAL', 'SELF_SERVICE', 'API', 'IMPORT')),
    created_at           timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (tenant_id, id),
    FOREIGN KEY (tenant_id, service_type_id) REFERENCES service_type (tenant_id, id),
    CONSTRAINT booking_time_order CHECK (end_at > start_at)
);

CREATE TABLE resource_assignment (
    tenant_id      uuid NOT NULL,
    id             uuid NOT NULL,
    booking_id     uuid NOT NULL,
    requirement_id uuid NOT NULL,
    resource_id    uuid NOT NULL,
    policy         text NOT NULL DEFAULT 'REASSIGNABLE'
                       CHECK (policy IN ('REASSIGNABLE', 'LOCKED', 'LOCKED_BY_POLICY')),
    PRIMARY KEY (tenant_id, id),
    FOREIGN KEY (tenant_id, booking_id) REFERENCES booking (tenant_id, id),
    FOREIGN KEY (tenant_id, requirement_id) REFERENCES resource_requirement (tenant_id, id),
    FOREIGN KEY (tenant_id, resource_id) REFERENCES resource (tenant_id, id),
    -- One assignment per requirement per booking (PRD-BKG-005 feasibility shape).
    CONSTRAINT resource_assignment_unique UNIQUE (tenant_id, booking_id, requirement_id)
);
