-- The audit log: one row per thing that happened. Rows are only ever added.
-- No foreign keys on purpose: an entry must outlive what it is about (a deleted user, a removed unit),
-- and must never stop that deletion either.
CREATE TABLE audit_entry (
    id            UUID          NOT NULL,
    tenant_id     UUID          NOT NULL,
    occurred_at   TIMESTAMPTZ   NOT NULL,
    actor_id      UUID          NULL,          -- NULL: nobody was logged in (a sign-up, startup)
    actor_email   VARCHAR(320)  NULL,
    action        VARCHAR(64)   NOT NULL,      -- identity.role.granted
    subject_type  VARCHAR(32)   NOT NULL,      -- user, person, org-unit
    subject_id    UUID          NOT NULL,
    summary       VARCHAR(500)  NOT NULL,
    request_id    VARCHAR(64)   NULL,
    PRIMARY KEY (id)
);

-- The two questions asked: "what happened lately?" and "what happened to this one?", both newest first
CREATE INDEX ix_audit_entry_tenant_time ON audit_entry (tenant_id, occurred_at DESC, id DESC);
CREATE INDEX ix_audit_entry_tenant_subject ON audit_entry (tenant_id, subject_id, occurred_at DESC, id DESC);

-- Append-only, enforced by the database: no code path, bug or SQL console can quietly edit history.
-- (Removing old entries for a retention policy would be a deliberate migration that drops this trigger first.)
CREATE FUNCTION audit_refuse_change() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit entries cannot be changed or deleted';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_entry_append_only BEFORE UPDATE OR DELETE ON audit_entry
    FOR EACH ROW EXECUTE FUNCTION audit_refuse_change();
