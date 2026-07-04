-- Stores a compressed base64-encoded image uploaded through the platform admin UI.
-- Preferred over logo_url when present; used as the sidebar logo and app watermark.
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS logo_data TEXT;
