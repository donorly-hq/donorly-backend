-- OTP codes are now stored as SHA-256 hex digests (64 chars), not raw digits.
ALTER TABLE auth_tokens ALTER COLUMN code TYPE VARCHAR(64);
