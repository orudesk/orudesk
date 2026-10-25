-- Fixed UUIDs, so every machine gets the same IDs and tests can rely on them.
INSERT INTO iam_tenant (id, code, name)
VALUES ('00000000-0000-0000-0000-000000000001', 'demo', 'Demo Organisation');

INSERT INTO iam_role (id, code, name, description) VALUES
                                                       ('00000000-0000-0000-0000-000000000101', 'ADMIN', 'Administrator', 'Full access, manages users and roles'),
                                                       ('00000000-0000-0000-0000-000000000102', 'USER',  'User',          'Default role for every registered user');