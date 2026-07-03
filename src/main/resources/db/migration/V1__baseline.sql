-- Donorly multi-tenant baseline schema (Design Doc Phase 0 + Phase 1)
-- All tenant-owned tables carry organization_id. UUID primary keys throughout.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================================================================
-- Phase 0: Platform, tenants, identity, RBAC
-- =========================================================================

CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(120) NOT NULL UNIQUE,
    vertical VARCHAR(50) NOT NULL DEFAULT 'nonprofit',
    status VARCHAR(30) NOT NULL DEFAULT 'trial'
        CHECK (status IN ('trial','active','suspended','cancelled')),
    logo_url TEXT,
    primary_color VARCHAR(20),
    timezone VARCHAR(80) NOT NULL DEFAULT 'America/Chicago',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE TABLE organization_settings (
    organization_id UUID PRIMARY KEY REFERENCES organizations(id),
    receipt_prefix VARCHAR(20),
    default_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    ai_enabled BOOLEAN NOT NULL DEFAULT false,
    payment_enabled BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    phone VARCHAR(30),
    password_hash TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'active'
        CHECK (status IN ('invited','active','disabled')),
    is_platform_admin BOOLEAN NOT NULL DEFAULT false,
    active_session_token VARCHAR(100),
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_users_email ON users (lower(email));

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    scope VARCHAR(30) NOT NULL DEFAULT 'organization'
        CHECK (scope IN ('platform','organization')),
    is_system BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id),
    permission_id UUID NOT NULL REFERENCES permissions(id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE organization_memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    user_id UUID NOT NULL REFERENCES users(id),
    role_id UUID NOT NULL REFERENCES roles(id),
    status VARCHAR(30) NOT NULL DEFAULT 'active'
        CHECK (status IN ('invited','active','disabled')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (organization_id, user_id)
);
CREATE INDEX idx_memberships_user ON organization_memberships (user_id);
CREATE INDEX idx_memberships_org ON organization_memberships (organization_id);

CREATE TABLE invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    email VARCHAR(255) NOT NULL,
    role_id UUID NOT NULL REFERENCES roles(id),
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending','accepted','revoked','expired')),
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_invitations_org ON invitations (organization_id);

-- =========================================================================
-- Phase 1: Donors
-- =========================================================================

CREATE TABLE donors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    full_name VARCHAR(200) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(30),
    city VARCHAR(120),
    donor_type VARCHAR(30) NOT NULL DEFAULT 'individual'
        CHECK (donor_type IN ('individual','family','business','anonymous')),
    status VARCHAR(30) NOT NULL DEFAULT 'active'
        CHECK (status IN ('active','inactive','do_not_contact','merged')),
    lifetime_giving NUMERIC(14,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_donors_org_name ON donors (organization_id, full_name);
CREATE UNIQUE INDEX uq_donors_org_email ON donors (organization_id, lower(email)) WHERE email IS NOT NULL;

CREATE TABLE donor_profiles (
    donor_id UUID PRIMARY KEY REFERENCES donors(id),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    occupation VARCHAR(200),
    employer VARCHAR(200),
    preferred_language VARCHAR(50),
    preferred_channel VARCHAR(30),
    notes_private TEXT
);

CREATE TABLE donor_tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(80) NOT NULL,
    color VARCHAR(20),
    UNIQUE (organization_id, name)
);

CREATE TABLE donor_tag_assignments (
    donor_id UUID NOT NULL REFERENCES donors(id),
    tag_id UUID NOT NULL REFERENCES donor_tags(id),
    PRIMARY KEY (donor_id, tag_id)
);

CREATE TABLE donor_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    donor_id UUID NOT NULL REFERENCES donors(id),
    note_text TEXT NOT NULL,
    note_type VARCHAR(40),
    visibility VARCHAR(20) NOT NULL DEFAULT 'team'
        CHECK (visibility IN ('private','team')),
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_donor_notes_donor ON donor_notes (donor_id);

-- =========================================================================
-- Phase 1: Campaigns
-- =========================================================================

CREATE TABLE campaigns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    campaign_type VARCHAR(50) NOT NULL DEFAULT 'general',
    goal_amount NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (goal_amount >= 0),
    start_date DATE,
    end_date DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'draft'
        CHECK (status IN ('draft','active','paused','completed','archived')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (organization_id, slug)
);
CREATE INDEX idx_campaigns_org ON campaigns (organization_id);

CREATE TABLE campaign_milestones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    campaign_id UUID NOT NULL REFERENCES campaigns(id),
    title VARCHAR(200) NOT NULL,
    target_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    target_date DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'pending'
);

-- =========================================================================
-- Phase 1: Pledges, pledge cards, follow-ups
-- =========================================================================

CREATE TABLE pledges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    campaign_id UUID NOT NULL REFERENCES campaigns(id),
    donor_id UUID NOT NULL REFERENCES donors(id),
    amount NUMERIC(14,2) NOT NULL CHECK (amount > 0),
    collected_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    frequency VARCHAR(30) NOT NULL DEFAULT 'one_time'
        CHECK (frequency IN ('one_time','weekly','monthly','quarterly','yearly')),
    payment_method VARCHAR(50),
    start_date DATE,
    end_date DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending','active','fulfilled','cancelled','defaulted')),
    source VARCHAR(50),
    notes TEXT,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_pledges_org_campaign ON pledges (organization_id, campaign_id);
CREATE INDEX idx_pledges_org_donor ON pledges (organization_id, donor_id);

CREATE TABLE pledge_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    pledge_id UUID NOT NULL REFERENCES pledges(id),
    due_date DATE NOT NULL,
    amount_due NUMERIC(14,2) NOT NULL CHECK (amount_due > 0),
    status VARCHAR(30) NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending','paid','overdue','cancelled')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_pledge_schedules_pledge ON pledge_schedules (pledge_id);

CREATE TABLE pledge_cards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    campaign_id UUID REFERENCES campaigns(id),
    donor_id UUID REFERENCES donors(id),
    image_url TEXT,
    extracted_json JSONB,
    amount NUMERIC(14,2),
    payment_method VARCHAR(50),
    notes TEXT,
    verification_status VARCHAR(30) NOT NULL DEFAULT 'pending'
        CHECK (verification_status IN ('pending','reviewed','approved','rejected')),
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_pledge_cards_org ON pledge_cards (organization_id);

CREATE TABLE donor_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    donor_id UUID NOT NULL REFERENCES donors(id),
    ambassador_user_id UUID NOT NULL REFERENCES users(id),
    campaign_id UUID REFERENCES campaigns(id),
    status VARCHAR(30) NOT NULL DEFAULT 'active'
        CHECK (status IN ('active','completed','released')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (donor_id, campaign_id, ambassador_user_id)
);
CREATE INDEX idx_donor_assignments_ambassador ON donor_assignments (organization_id, ambassador_user_id);

CREATE TABLE follow_ups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    donor_id UUID NOT NULL REFERENCES donors(id),
    campaign_id UUID REFERENCES campaigns(id),
    assigned_to_user_id UUID REFERENCES users(id),
    due_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL DEFAULT 'open'
        CHECK (status IN ('open','completed','cancelled','overdue')),
    outcome VARCHAR(40),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_follow_ups_org_status ON follow_ups (organization_id, status);
CREATE INDEX idx_follow_ups_assignee ON follow_ups (assigned_to_user_id);

-- =========================================================================
-- Operational: audit logs
-- =========================================================================

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID REFERENCES organizations(id),
    user_id UUID REFERENCES users(id),
    action VARCHAR(120) NOT NULL,
    entity_type VARCHAR(80),
    entity_id UUID,
    ip_address VARCHAR(64),
    user_agent VARCHAR(400),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_logs_org ON audit_logs (organization_id, created_at);
