-- Lead / setup / cleanup buffers (PRD-SVC-009, PRD-RES-012, RES-AT-005).
-- The appointment window stays client-facing; a resource is reserved for the buffered window
-- [start − lead − setup, end + cleanup), which is what the exclusion constraint contends on.

ALTER TABLE resource
    ADD COLUMN setup_minutes   int NOT NULL DEFAULT 0 CHECK (setup_minutes >= 0),
    ADD COLUMN cleanup_minutes int NOT NULL DEFAULT 0 CHECK (cleanup_minutes >= 0);

ALTER TABLE service_type
    ADD COLUMN lead_minutes int NOT NULL DEFAULT 0 CHECK (lead_minutes >= 0);
