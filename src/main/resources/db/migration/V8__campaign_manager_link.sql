-- V8: link campaigns to their managing user + support volunteer creation permission

ALTER TABLE campaigns
    ADD COLUMN IF NOT EXISTS managed_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_campaigns_managed_by ON campaigns(managed_by_user_id);
