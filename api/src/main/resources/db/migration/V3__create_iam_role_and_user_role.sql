-- Roles are rows, not a Java enum, so verticals can add their own roles.
CREATE TABLE iam_role (
                          id           UUID          NOT NULL,
                          code         VARCHAR(50)   NOT NULL,
                          name         VARCHAR(100)  NOT NULL,
                          description  VARCHAR(500)  NULL,
                          PRIMARY KEY (id),
                          CONSTRAINT uk_iam_role_code UNIQUE (code)
);

-- Join table for the many-to-many link: one user has many roles, one role has many users.
CREATE TABLE iam_user_role (
                               user_id      UUID         NOT NULL,
                               role_id      UUID         NOT NULL,
                               assigned_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               PRIMARY KEY (user_id, role_id),
                               CONSTRAINT fk_iam_user_role_user FOREIGN KEY (user_id) REFERENCES iam_user (id) ON DELETE CASCADE,
                               CONSTRAINT fk_iam_user_role_role FOREIGN KEY (role_id) REFERENCES iam_role (id) ON DELETE RESTRICT
);
