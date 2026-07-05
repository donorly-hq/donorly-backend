-- Allow the org-selection login challenge purpose on auth_tokens.
ALTER TABLE auth_tokens DROP CONSTRAINT IF EXISTS auth_tokens_purpose_check;
ALTER TABLE auth_tokens ADD CONSTRAINT auth_tokens_purpose_check
    CHECK (purpose IN ('password_reset', 'login_otp', 'org_select'));
