-- Donorly: Townhall meetings
-- Tenant-scoped record of a townhall / one-on-one meeting with a person.

CREATE TABLE townhalls (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    person_name VARCHAR(200) NOT NULL,
    address VARCHAR(300),
    event_date DATE,
    event_time TIME,
    duration_minutes INTEGER CHECK (duration_minutes IS NULL OR duration_minutes > 0),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_townhalls_org ON townhalls (organization_id);
CREATE INDEX idx_townhalls_org_date ON townhalls (organization_id, event_date);
