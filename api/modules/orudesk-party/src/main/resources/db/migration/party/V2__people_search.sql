-- People search (Day 16): find a person by any part of their name, phone or email, quickly, in a big registry.

-- pg_trgm splits text into trigrams ("asha" → "  a", " as", "ash", "sha", "ha ") and can index them,
-- so LIKE '%sha%' uses an index instead of reading every row. It ships with PostgreSQL and is a
-- "trusted" extension: the database owner may enable it without being a superuser.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- One lower-cased text to search in, kept up to date by PostgreSQL itself on every insert and update.
-- A generated column can't drift from the columns it's made of, and the application never writes it.
ALTER TABLE party_person
    ADD COLUMN search_text TEXT GENERATED ALWAYS AS (
        lower(first_name || ' ' || last_name || ' ' || coalesce(phone, '') || ' ' || coalesce(email, ''))
    ) STORED;

-- GIN: an inverted index, "trigram → rows that contain it". The right kind for "contains" searches.
CREATE INDEX ix_party_person_search ON party_person USING gin (search_text gin_trgm_ops);

-- The list is sorted by (last_name, first_name, id), and pages continue "after" the last row seen.
-- Adding id makes the order unique, so no person is skipped or shown twice between pages.
DROP INDEX ix_party_person_tenant_name;
CREATE INDEX ix_party_person_tenant_name ON party_person (tenant_id, last_name, first_name, id);
