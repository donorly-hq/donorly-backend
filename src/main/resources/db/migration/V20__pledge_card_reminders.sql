-- V20: AI-automated pledge card reminders with donor response capture
-- Reminder tracking on cards, tokenized donor action links, and the
-- per-org reminder policy.

-- ===== pledge cards: reminder tracking ======================================

ALTER TABLE pledge_cards
    ADD COLUMN IF NOT EXISTS last_reminder_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS reminders_paused BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS promised_date DATE;

-- ===== pledges: donor opt-out for reminder emails ==========================

ALTER TABLE pledges
    ADD COLUMN IF NOT EXISTS reminders_paused BOOLEAN NOT NULL DEFAULT false;

-- ===== donor action tokens ==================================================
-- One-time links embedded in reminder emails ("I already paid" / "I'll pay
-- by ..." / "stop reminders"). Separate from auth_tokens: donors are not users.
-- A token points at either a pledge card or a pledge (both flows send reminders).

CREATE TABLE donor_action_tokens (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID         NOT NULL REFERENCES organizations(id),
    pledge_card_id  UUID         REFERENCES pledge_cards(id) ON DELETE CASCADE,
    pledge_id       UUID         REFERENCES pledges(id) ON DELETE CASCADE,
    donor_id        UUID         NOT NULL REFERENCES donors(id) ON DELETE CASCADE,
    token           VARCHAR(100) NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ  NOT NULL,
    used_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_donor_action_tokens_target
        CHECK (num_nonnulls(pledge_card_id, pledge_id) = 1)
);

CREATE INDEX idx_donor_action_tokens_card ON donor_action_tokens (pledge_card_id);
CREATE INDEX idx_donor_action_tokens_pledge ON donor_action_tokens (pledge_id);

-- ===== org settings: reminder policy ========================================

ALTER TABLE organization_settings
    ADD COLUMN IF NOT EXISTS pledge_card_reminders_enabled BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS pledge_card_reminder_interval_days INT NOT NULL DEFAULT 3,
    ADD COLUMN IF NOT EXISTS pledge_card_reminder_max INT NOT NULL DEFAULT 3;
