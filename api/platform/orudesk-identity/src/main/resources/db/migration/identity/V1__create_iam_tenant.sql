-- A tenant is one organisation using Orudesk (a hospital, an NGO, a school).
CREATE TABLE iam_tenant (
                            id          UUID          NOT NULL,
                            code        VARCHAR(50)   NOT NULL,
                            name        VARCHAR(200)  NOT NULL,
                            enabled     BOOLEAN       NOT NULL DEFAULT TRUE,
                            created_at  TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at  TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (id),
                            CONSTRAINT uk_iam_tenant_code UNIQUE (code)
);

-- PostgreSQL has no ON UPDATE CURRENT_TIMESTAMP, so a trigger keeps updated_at current.
-- The function belongs to identity (iam_ prefix), like its tables.
CREATE FUNCTION iam_set_updated_at() RETURNS trigger AS $$
BEGIN
    NEW.updated_at := CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_iam_tenant_updated_at BEFORE UPDATE ON iam_tenant
    FOR EACH ROW EXECUTE FUNCTION iam_set_updated_at();
