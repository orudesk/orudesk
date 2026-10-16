-- A user is a login. Each user belongs to exactly one tenant.
CREATE TABLE iam_user (
                          id             UUID          NOT NULL,
                          tenant_id      UUID          NOT NULL,
                          email          VARCHAR(320)  NOT NULL,
                          password_hash  VARCHAR(100)  NOT NULL,
                          enabled        BOOLEAN       NOT NULL DEFAULT TRUE,
                          created_at     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                          updated_at     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                          PRIMARY KEY (id),
                          CONSTRAINT uk_iam_user_email UNIQUE (email),
                          CONSTRAINT fk_iam_user_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id)
);