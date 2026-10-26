-- Organisation units (Day 18): the schools, hospitals, branches and groups inside one tenant, as a tree.
-- The tenant is the customer (a trust, a school group) and the hard wall between customers. Org units are
-- how that customer is organised inside the wall, and roles can be granted per unit (Day 19).
CREATE TABLE iam_org_unit (
    id          UUID          NOT NULL,
    tenant_id   UUID          NOT NULL,
    parent_id   UUID          NULL,          -- NULL only for the tenant's root unit
    type        VARCHAR(50)   NOT NULL,      -- ORGANISATION, GROUP, SCHOOL, HOSPITAL, BRANCH... (rows, not an enum)
    code        VARCHAR(50)   NOT NULL,
    name        VARCHAR(200)  NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    -- Lets the parent foreign key below include the tenant (a foreign key must point at a unique key)
    CONSTRAINT uk_iam_org_unit_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_iam_org_unit_code UNIQUE (tenant_id, code),
    CONSTRAINT fk_iam_org_unit_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id),
    -- The parent must exist AND be in the same tenant: the database itself refuses a cross-tenant tree
    CONSTRAINT fk_iam_org_unit_parent FOREIGN KEY (tenant_id, parent_id) REFERENCES iam_org_unit (tenant_id, id),
    CONSTRAINT ck_iam_org_unit_not_own_parent CHECK (parent_id <> id)
);

-- A partial unique index: at most one row per tenant WHERE parent_id IS NULL, so one root per tenant.
CREATE UNIQUE INDEX ux_iam_org_unit_one_root ON iam_org_unit (tenant_id) WHERE parent_id IS NULL;

-- "The children of this unit": used when walking the tree, and by the foreign key when a parent is deleted.
CREATE INDEX ix_iam_org_unit_parent ON iam_org_unit (tenant_id, parent_id);

CREATE TRIGGER trg_iam_org_unit_updated_at BEFORE UPDATE ON iam_org_unit
    FOR EACH ROW EXECUTE FUNCTION iam_set_updated_at();

-- The demo tenant's root unit. Every tenant has exactly one; new tenants will get theirs at sign-up.
INSERT INTO iam_org_unit (id, tenant_id, parent_id, type, code, name)
VALUES ('00000000-0000-0000-0000-000000000201', '00000000-0000-0000-0000-000000000001', NULL,
        'ORGANISATION', 'demo', 'Demo Organisation');
