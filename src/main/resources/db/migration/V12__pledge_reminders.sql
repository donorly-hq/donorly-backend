-- Track when a donor was last reminded about an unfulfilled pledge,
-- so the daily reminder job doesn't nag on every run.
ALTER TABLE pledges ADD COLUMN last_reminder_at TIMESTAMPTZ;
