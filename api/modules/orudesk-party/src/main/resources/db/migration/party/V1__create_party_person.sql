-- The People registry: one row per person an organisation deals with (a patient, donor, student, guardian...).
-- Verticals add their own details in their own tables, pointing at this one (hc_patient.person_id → party_person.id).
CREATE TABLE party_person (
    id             UUID          NOT NULL,
    tenant_id      UUID          NOT NULL,
    first_name     VARCHAR(100)  NOT NULL,
    last_name      VARCHAR(100)  NOT NULL,
    date_of_birth  DATE          NULL,
    phone          VARCHAR(30)   NULL,
    email          VARCHAR(320)  NULL,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_party_person_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id)
);

-- Every lookup is "within my tenant", and the list is sorted by name (search comes on Day 16).
CREATE INDEX ix_party_person_tenant_name ON party_person (tenant_id, last_name, first_name);

-- Keeps updated_at current (PostgreSQL has no ON UPDATE). Party owns its own function, so it never
-- depends on identity's.
CREATE FUNCTION party_set_updated_at() RETURNS trigger AS $$
BEGIN
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_party_person_updated_at BEFORE UPDATE ON party_person
    FOR EACH ROW EXECUTE FUNCTION party_set_updated_at();
