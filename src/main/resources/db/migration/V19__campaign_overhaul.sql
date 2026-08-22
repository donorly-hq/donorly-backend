-- V19: campaign/donor/payments/pledge-card overhaul skeleton
-- Donor targeting data, campaign messaging + audiences, pledge-card workflow
-- fields, campaign-linked payments, and communication channel extensions.

-- ===== donors: location, POC, buckets, compliance ==========================

ALTER TABLE donors
    ADD COLUMN IF NOT EXISTS state VARCHAR(100),
    ADD COLUMN IF NOT EXISTS address TEXT,
    ADD COLUMN IF NOT EXISTS assigned_to_user_id UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS is_major_donor BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS bucket VARCHAR(30) NOT NULL DEFAULT 'confirmed',
    ADD COLUMN IF NOT EXISTS compliance_status VARCHAR(30) NOT NULL DEFAULT 'ok';

ALTER TABLE donors
    ADD CONSTRAINT chk_donors_bucket
        CHECK (bucket IN ('confirmed','potential','re_registering')),
    ADD CONSTRAINT chk_donors_compliance
        CHECK (compliance_status IN ('ok','non_compliant','claims_paid','non_responsive'));

CREATE INDEX idx_donors_org_state ON donors (organization_id, state);
CREATE INDEX idx_donors_org_bucket ON donors (organization_id, bucket);

-- ===== campaign messaging config (1:1 with campaign) =======================

CREATE TABLE campaign_messaging (
    campaign_id     UUID         PRIMARY KEY REFERENCES campaigns(id) ON DELETE CASCADE,
    organization_id UUID         NOT NULL REFERENCES organizations(id),
    message_content TEXT,
    flyer_url       TEXT,
    payment_link    TEXT,
    frequency       VARCHAR(20)  NOT NULL DEFAULT 'weekly'
        CHECK (frequency IN ('daily','every_2_days','weekly')),
    -- comma-separated subset of: whatsapp,sms,robocall,email
    channels        VARCHAR(120) NOT NULL DEFAULT 'email',
    personalized    BOOLEAN      NOT NULL DEFAULT true,
    last_sent_at    TIMESTAMPTZ,
    -- audit
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      UUID         REFERENCES users(id),
    modified_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    modified_by     UUID         REFERENCES users(id)
);

-- ===== campaign audience targeting ==========================================

CREATE TABLE campaign_targets (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID         NOT NULL REFERENCES organizations(id),
    campaign_id     UUID         NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    -- exactly one of the three selectors
    donor_id        UUID         REFERENCES donors(id) ON DELETE CASCADE,
    tag_id          UUID         REFERENCES donor_tags(id) ON DELETE CASCADE,
    state           VARCHAR(100),
    -- audit
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      UUID         REFERENCES users(id),
    modified_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    modified_by     UUID         REFERENCES users(id),
    CONSTRAINT chk_campaign_targets_one_selector
        CHECK (num_nonnulls(donor_id, tag_id, state) = 1)
);

CREATE INDEX idx_campaign_targets_campaign ON campaign_targets (campaign_id);
CREATE UNIQUE INDEX uq_campaign_targets_donor
    ON campaign_targets (campaign_id, donor_id) WHERE donor_id IS NOT NULL;
CREATE UNIQUE INDEX uq_campaign_targets_tag
    ON campaign_targets (campaign_id, tag_id) WHERE tag_id IS NOT NULL;
CREATE UNIQUE INDEX uq_campaign_targets_state
    ON campaign_targets (campaign_id, state) WHERE state IS NOT NULL;

-- ===== pledge cards: POC, follow-ups, auto-approve, pilot batch ============

ALTER TABLE pledge_cards
    ADD COLUMN IF NOT EXISTS point_of_contact_user_id UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS follow_up_count INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS pending_since TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS batch VARCHAR(60);

UPDATE pledge_cards SET pending_since = created_at WHERE pending_since IS NULL;

ALTER TABLE pledge_cards DROP CONSTRAINT IF EXISTS pledge_cards_verification_status_check;
ALTER TABLE pledge_cards
    ADD CONSTRAINT pledge_cards_verification_status_check
        CHECK (verification_status IN
            ('pending','reviewed','approved','rejected',
             'needs_verification','claims_paid','non_responsive'));

CREATE INDEX idx_pledge_cards_org_status ON pledge_cards (organization_id, verification_status);
CREATE INDEX idx_pledge_cards_org_batch ON pledge_cards (organization_id, batch);

-- ===== payments: direct campaign link (takaza / no-pledge donations) =======

ALTER TABLE payments
    ALTER COLUMN pledge_id DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS campaign_id UUID REFERENCES campaigns(id);

-- Backfill campaign from the pledge for existing rows.
UPDATE payments p
SET campaign_id = pl.campaign_id
FROM pledges pl
WHERE p.pledge_id = pl.id AND p.campaign_id IS NULL;

-- Every payment must land somewhere: a pledge, a campaign, or both.
ALTER TABLE payments
    ADD CONSTRAINT chk_payments_pledge_or_campaign
        CHECK (pledge_id IS NOT NULL OR campaign_id IS NOT NULL);

CREATE INDEX idx_payments_campaign ON payments (campaign_id);

-- ===== communication messages: campaigns, direction, Twilio correlation ====

ALTER TABLE communication_messages
    ADD COLUMN IF NOT EXISTS campaign_id UUID REFERENCES campaigns(id),
    ADD COLUMN IF NOT EXISTS direction VARCHAR(10) NOT NULL DEFAULT 'outbound',
    ADD COLUMN IF NOT EXISTS external_id VARCHAR(120);

ALTER TABLE communication_messages
    ADD CONSTRAINT chk_comm_messages_direction CHECK (direction IN ('outbound','inbound'));

ALTER TABLE communication_messages DROP CONSTRAINT IF EXISTS communication_messages_channel_check;
ALTER TABLE communication_messages
    ADD CONSTRAINT communication_messages_channel_check
        CHECK (channel IN ('email','sms','whatsapp','robocall'));

ALTER TABLE communication_messages DROP CONSTRAINT IF EXISTS communication_messages_status_check;
ALTER TABLE communication_messages
    ADD CONSTRAINT communication_messages_status_check
        CHECK (status IN ('queued','sent','failed','skipped','received'));

CREATE INDEX idx_comm_messages_campaign ON communication_messages (campaign_id);

-- ===== org settings: pledge-card auto-approval policy ======================

ALTER TABLE organization_settings
    ADD COLUMN IF NOT EXISTS pledge_card_auto_approve BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS pledge_card_auto_approve_hours INT NOT NULL DEFAULT 24;
