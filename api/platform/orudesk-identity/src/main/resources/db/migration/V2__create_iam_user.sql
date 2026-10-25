-- A user is a login. Each user belongs to exactly one tenant.
CREATE TABLE iam_user (
                          id             UUID          NOT NULL,
                          tenant_id      UUID          NOT NULL,
                          email          VARCHAR(320)  NOT NULL,
                          password_hash  VARCHAR(100)  NOT NULL,
                          enabled        BOOLEAN       NOT NULL DEFAULT TRUE,
                          created_at     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          PRIMARY KEY (id),
                          CONSTRAINT uk_iam_user_email UNIQUE (email),
                          CONSTRAINT fk_iam_user_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id)
);

CREATE TRIGGER trg_iam_user_updated_at BEFORE UPDATE ON iam_user
    FOR EACH ROW EXECUTE FUNCTION iam_set_updated_at();
