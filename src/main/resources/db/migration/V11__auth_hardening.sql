-- Auth hardening: multi-session support, password reset, email OTP (2FA)

-- Multiple concurrent sessions per user (replaces users.active_session_token)
CREATE TABLE user_sessions (
    jti VARCHAR(64) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_user_sessions_user ON user_sessions (user_id);

-- Carry over currently active sessions so nobody is logged out by this deploy
INSERT INTO user_sessions (jti, user_id, expires_at)
SELECT active_session_token, id, now() + interval '24 hours'
FROM users
WHERE active_session_token IS NOT NULL;

-- Short-lived tokens: password reset links and login OTP codes
CREATE TABLE auth_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(128) NOT NULL UNIQUE,
    purpose VARCHAR(32) NOT NULL
        CHECK (purpose IN ('password_reset', 'login_otp')),
    code VARCHAR(16),
    context VARCHAR(255),
    attempts INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_auth_tokens_user ON auth_tokens (user_id);
