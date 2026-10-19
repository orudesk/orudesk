package com.orudesk.orudesk.identity.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** One row of iam_role. Only the persistence adapter knows this class exists. */
@Entity
@Table(name = "iam_role")
class RoleJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    protected RoleJpaEntity() {
        // JPA needs a no-argument constructor
    }

    UUID getId() { return id; }
    String getCode() { return code; }
}
