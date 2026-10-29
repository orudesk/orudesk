-- Password reset tokens (Day 53). Only the SHA-256 hash of a token is stored, like refresh tokens: someone who
-- reads this table (a backup, a support query) can't reset anybody's password with what they find.
-- No tenant_id: a token belongs to a user, and the user row already says which tenant.
CREATE TABLE iam_password_reset (
    token_hash  CHAR(64)                  NOT NULL PRIMARY KEY,
    user_id     UUID                      NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE  NOT NULL,
    expires_at  TIMESTAMP WITH TIME ZONE  NOT NULL,
    used_at     TIMESTAMP WITH TIME ZONE  NULL,
    CONSTRAINT fk_iam_password_reset_user FOREIGN KEY (user_id) REFERENCES iam_user (id) ON DELETE CASCADE,
    CONSTRAINT ck_iam_password_reset_expiry CHECK (expires_at > created_at)
);

-- "This user's other open tokens" (deleted when a new one is issued) and the foreign key's own lookups.
CREATE INDEX ix_iam_password_reset_user ON iam_password_reset (user_id);
