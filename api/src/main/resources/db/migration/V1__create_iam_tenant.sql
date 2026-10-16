-- A tenant is one organisation using Orudesk (a hospital, an NGO, a school).
CREATE TABLE iam_tenant (
                            id          UUID          NOT NULL,
                            code        VARCHAR(50)   NOT NULL,
                            name        VARCHAR(200)  NOT NULL,
                            enabled     BOOLEAN       NOT NULL DEFAULT TRUE,
                            created_at  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                            updated_at  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                            PRIMARY KEY (id),
                            CONSTRAINT uk_iam_tenant_code UNIQUE (code)
);