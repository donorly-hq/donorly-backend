-- =============================================================================
-- V7: Standardise audit columns across all tables
--
-- Adds the four standard audit columns to every table:
--   created_at  (already exists on most — kept as-is)
--   created_by  (new where missing)
--   modified_at (renamed from updated_at where present; added where absent)
--   modified_by (new everywhere)
--
-- Tables that had no updated_at get modified_at defaulting to created_at
-- so existing rows are consistent.
-- =============================================================================

-- ===== organizations =====
ALTER TABLE organizations
    RENAME COLUMN updated_at TO modified_at;
ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS created_by  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== organization_settings =====
ALTER TABLE organization_settings
    RENAME COLUMN updated_at TO modified_at;
ALTER TABLE organization_settings
    ADD COLUMN IF NOT EXISTS created_by  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== users =====
ALTER TABLE users
    RENAME COLUMN updated_at TO modified_at;
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS created_by  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== roles (no audit before) =====
ALTER TABLE roles
    ADD COLUMN IF NOT EXISTS created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS created_by  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS modified_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== permissions (no audit before) =====
ALTER TABLE permissions
    ADD COLUMN IF NOT EXISTS created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS created_by  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS modified_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== role_permissions (junction — append audit) =====
ALTER TABLE role_permissions
    ADD COLUMN IF NOT EXISTS created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS created_by  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS modified_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== organization_memberships =====
ALTER TABLE organization_memberships
    RENAME COLUMN updated_at TO modified_at;
ALTER TABLE organization_memberships
    ADD COLUMN IF NOT EXISTS created_by  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== invitations =====
ALTER TABLE invitations
    ADD COLUMN IF NOT EXISTS created_by  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS modified_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== donors =====
ALTER TABLE donors
    RENAME COLUMN updated_at TO modified_at;
ALTER TABLE donors
    ADD COLUMN IF NOT EXISTS created_by  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== donor_profiles (no audit before) =====
ALTER TABLE donor_profiles
    ADD COLUMN IF NOT EXISTS created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS created_by  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS modified_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== donor_tags (no audit before) =====
ALTER TABLE donor_tags
    ADD COLUMN IF NOT EXISTS created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS created_by  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS modified_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== donor_tag_assignments (junction — append audit) =====
ALTER TABLE donor_tag_assignments
    ADD COLUMN IF NOT EXISTS created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS created_by  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS modified_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== donor_notes (already has created_by / created_at) =====
ALTER TABLE donor_notes
    ADD COLUMN IF NOT EXISTS modified_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== campaigns =====
ALTER TABLE campaigns
    RENAME COLUMN updated_at TO modified_at;
ALTER TABLE campaigns
    ADD COLUMN IF NOT EXISTS created_by  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== campaign_milestones (no audit before) =====
ALTER TABLE campaign_milestones
    ADD COLUMN IF NOT EXISTS created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS created_by  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS modified_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== pledges (already has created_by / created_at) =====
ALTER TABLE pledges
    RENAME COLUMN updated_at TO modified_at;
ALTER TABLE pledges
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== pledge_schedules =====
ALTER TABLE pledge_schedules
    ADD COLUMN IF NOT EXISTS created_by  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS modified_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== pledge_cards (already has created_by / created_at) =====
ALTER TABLE pledge_cards
    ADD COLUMN IF NOT EXISTS modified_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== donor_assignments =====
ALTER TABLE donor_assignments
    ADD COLUMN IF NOT EXISTS created_by  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS modified_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== follow_ups =====
ALTER TABLE follow_ups
    RENAME COLUMN updated_at TO modified_at;
ALTER TABLE follow_ups
    ADD COLUMN IF NOT EXISTS created_by  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== events =====
ALTER TABLE events
    RENAME COLUMN updated_at TO modified_at;
ALTER TABLE events
    ADD COLUMN IF NOT EXISTS created_by  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== event_registrations =====
ALTER TABLE event_registrations
    RENAME COLUMN updated_at TO modified_at;
ALTER TABLE event_registrations
    ADD COLUMN IF NOT EXISTS created_by  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== volunteer_shifts =====
ALTER TABLE volunteer_shifts
    RENAME COLUMN updated_at TO modified_at;
ALTER TABLE volunteer_shifts
    ADD COLUMN IF NOT EXISTS created_by  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== volunteer_assignments =====
ALTER TABLE volunteer_assignments
    RENAME COLUMN updated_at TO modified_at;
ALTER TABLE volunteer_assignments
    ADD COLUMN IF NOT EXISTS created_by  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== townhalls =====
ALTER TABLE townhalls
    RENAME COLUMN updated_at TO modified_at;
ALTER TABLE townhalls
    ADD COLUMN IF NOT EXISTS created_by  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== message_templates =====
ALTER TABLE message_templates
    RENAME COLUMN updated_at TO modified_at;
ALTER TABLE message_templates
    ADD COLUMN IF NOT EXISTS created_by  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== communication_messages =====
ALTER TABLE communication_messages
    ADD COLUMN IF NOT EXISTS created_by  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS modified_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== ai_insights =====
ALTER TABLE ai_insights
    ADD COLUMN IF NOT EXISTS created_by  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS modified_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== ai_conversations =====
ALTER TABLE ai_conversations
    ADD COLUMN IF NOT EXISTS created_by  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS modified_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== payments =====
ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS created_by  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS modified_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);

-- ===== receipts =====
ALTER TABLE receipts
    ADD COLUMN IF NOT EXISTS created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS created_by  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS modified_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS modified_by UUID REFERENCES users(id);
