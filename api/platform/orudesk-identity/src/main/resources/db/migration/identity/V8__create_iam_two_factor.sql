-- Two-factor sign-in with an authenticator app (Day 67).
-- The TOTP secret can't be hashed (the server computes codes from it), so it's encrypted with AES-GCM under a key
-- that lives outside the database (ORUDESK_MFA_KEY): "v1:" + base64(nonce + ciphertext + tag).
CREATE TABLE iam_user_totp (
    user_id         UUID                      NOT NULL PRIMARY KEY,
    secret_sealed   VARCHAR(200)              NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE  NOT NULL,
    -- NULL until the user proves the app works with a first code: only then is two-factor sign-in on
    confirmed_at    TIMESTAMP WITH TIME ZONE  NULL,
    -- The 30-second step of the last code accepted: a code works once (RFC 6238 §5.2)
    last_used_step  BIGINT                    NOT NULL DEFAULT 0,
    CONSTRAINT fk_iam_user_totp_user FOREIGN KEY (user_id) REFERENCES iam_user (id) ON DELETE CASCADE
);

-- One-time recovery codes, as SHA-256 hashes. Used ones stay (used_at set) so the count left is honest.
CREATE TABLE iam_recovery_code (
    user_id    UUID                      NOT NULL,
    code_hash  CHAR(64)                  NOT NULL,
    used_at    TIMESTAMP WITH TIME ZONE  NULL,
    PRIMARY KEY (user_id, code_hash),
    CONSTRAINT fk_iam_recovery_code_user FOREIGN KEY (user_id) REFERENCES iam_user (id) ON DELETE CASCADE
);
