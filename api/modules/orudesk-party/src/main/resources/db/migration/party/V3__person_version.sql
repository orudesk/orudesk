-- Day 46: people can be edited. "version" counts the changes to a row; an edit says which version it was made
-- on, and is refused if the row has moved on since (optimistic locking). Existing rows start at 0.
-- Adding a NOT NULL column with a constant DEFAULT doesn't rewrite the table in PostgreSQL 11 and later: the
-- default is stored once in the catalogue, so this is instant even on a large table.
ALTER TABLE party_person ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
