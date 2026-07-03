-- V9: extend townhalls with phone, venue, host ambassador, expected RSVPs

ALTER TABLE townhalls
    ADD COLUMN IF NOT EXISTS phone           VARCHAR(50),
    ADD COLUMN IF NOT EXISTS venue           VARCHAR(255),
    ADD COLUMN IF NOT EXISTS host_ambassador_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS expected_rsvps  INTEGER;
