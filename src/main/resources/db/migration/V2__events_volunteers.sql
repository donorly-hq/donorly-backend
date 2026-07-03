-- Donorly Phase 4: Events & Volunteers
-- All tables carry organization_id for tenant isolation.

-- =========================================================================
-- Events
-- =========================================================================

CREATE TABLE events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    campaign_id UUID REFERENCES campaigns(id),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    event_type VARCHAR(50) NOT NULL DEFAULT 'general',
    location VARCHAR(300),
    starts_at TIMESTAMPTZ,
    ends_at TIMESTAMPTZ,
    capacity INTEGER,
    status VARCHAR(30) NOT NULL DEFAULT 'draft'
        CHECK (status IN ('draft','published','in_progress','completed','cancelled')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_events_org ON events (organization_id);
CREATE INDEX idx_events_org_starts ON events (organization_id, starts_at);

-- =========================================================================
-- Event registrations (guests / attendees)
-- =========================================================================

CREATE TABLE event_registrations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    event_id UUID NOT NULL REFERENCES events(id),
    donor_id UUID REFERENCES donors(id),
    guest_name VARCHAR(200) NOT NULL,
    guest_email VARCHAR(255),
    guest_phone VARCHAR(30),
    party_size INTEGER NOT NULL DEFAULT 1 CHECK (party_size > 0),
    status VARCHAR(30) NOT NULL DEFAULT 'registered'
        CHECK (status IN ('registered','checked_in','cancelled','no_show')),
    check_in_code VARCHAR(40) NOT NULL,
    checked_in_at TIMESTAMPTZ,
    checked_in_by UUID REFERENCES users(id),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_event_registrations_event ON event_registrations (event_id);
CREATE INDEX idx_event_registrations_org ON event_registrations (organization_id);
CREATE UNIQUE INDEX uq_event_registrations_code ON event_registrations (event_id, check_in_code);

-- =========================================================================
-- Volunteer shifts + assignments
-- =========================================================================

CREATE TABLE volunteer_shifts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    event_id UUID NOT NULL REFERENCES events(id),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    starts_at TIMESTAMPTZ,
    ends_at TIMESTAMPTZ,
    slots INTEGER NOT NULL DEFAULT 1 CHECK (slots > 0),
    status VARCHAR(30) NOT NULL DEFAULT 'open'
        CHECK (status IN ('open','full','closed','cancelled')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_volunteer_shifts_event ON volunteer_shifts (event_id);
CREATE INDEX idx_volunteer_shifts_org ON volunteer_shifts (organization_id);

CREATE TABLE volunteer_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    shift_id UUID NOT NULL REFERENCES volunteer_shifts(id),
    user_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(30) NOT NULL DEFAULT 'assigned'
        CHECK (status IN ('assigned','confirmed','checked_in','completed','cancelled')),
    checked_in_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (shift_id, user_id)
);
CREATE INDEX idx_volunteer_assignments_shift ON volunteer_assignments (shift_id);
CREATE INDEX idx_volunteer_assignments_user ON volunteer_assignments (organization_id, user_id);
