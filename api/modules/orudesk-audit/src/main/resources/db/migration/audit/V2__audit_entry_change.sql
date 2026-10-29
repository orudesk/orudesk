-- Day 47: for an edit, which fields changed and from what to what. One row per field, in the order of the form.
-- A table rather than a JSON column: "every change to anyone's phone number last month" is a plain indexed
-- query, and each value has a type and a length limit the database checks.
CREATE TABLE audit_entry_change (
    entry_id       UUID          NOT NULL,
    position       INTEGER       NOT NULL,      -- 0, 1, 2...: the order the module listed them in
    field          VARCHAR(64)   NOT NULL,      -- phone, dateOfBirth
    old_value      VARCHAR(500)  NULL,
    new_value      VARCHAR(500)  NULL,
    values_hidden  BOOLEAN       NOT NULL DEFAULT FALSE,   -- only the fact of the change is kept (a tax id)
    PRIMARY KEY (entry_id, position),
    -- Unlike audit_entry's subject, the entry is ours: a change can't exist without its entry
    CONSTRAINT fk_audit_entry_change_entry FOREIGN KEY (entry_id) REFERENCES audit_entry (id),
    CONSTRAINT ck_audit_entry_change_hidden CHECK (NOT values_hidden OR (old_value IS NULL AND new_value IS NULL))
);

-- Append-only, like the entries themselves (the same function: it only raises an error)
CREATE TRIGGER trg_audit_entry_change_append_only BEFORE UPDATE OR DELETE ON audit_entry_change
    FOR EACH ROW EXECUTE FUNCTION audit_refuse_change();
