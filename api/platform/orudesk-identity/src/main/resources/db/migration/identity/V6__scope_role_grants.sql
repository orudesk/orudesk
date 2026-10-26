-- Scoped roles (Day 19): a role can be granted for the whole tenant (org_unit_id NULL, as before),
-- or for one org unit and everything below it ("PRINCIPAL of Sunrise School A").
ALTER TABLE iam_user_role ADD COLUMN org_unit_id UUID NULL;

-- Can't delete a school while people still hold roles in it: remove the grants first, on purpose.
ALTER TABLE iam_user_role ADD CONSTRAINT fk_iam_user_role_org_unit
    FOREIGN KEY (org_unit_id) REFERENCES iam_org_unit (id);

-- The old key (user_id, role_id) allowed one ADMIN per user. Now a user may be ADMIN of School A and of
-- School B, so the key needs the unit too. A primary key can't contain a NULL, so the row gets its own id,
-- and a unique index keeps "the same role at the same place twice" impossible.
ALTER TABLE iam_user_role DROP CONSTRAINT iam_user_role_pkey;
ALTER TABLE iam_user_role ADD COLUMN id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE iam_user_role ADD PRIMARY KEY (id);

-- NULLS NOT DISTINCT (PostgreSQL 15+): two tenant-wide ADMIN grants (both NULL) count as duplicates.
-- Without it, NULL is "unknown", never equal to another NULL, and the duplicate would slip in.
CREATE UNIQUE INDEX ux_iam_user_role_grant ON iam_user_role (user_id, role_id, org_unit_id) NULLS NOT DISTINCT;
