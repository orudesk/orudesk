package com.orudesk.orudesk.identity.adapter.out.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

/** One row of iam_user, plus its role grants: the rows of iam_user_role. */
@Entity
@Table(name = "iam_user")
class UserJpaEntity {

    @Id
    private UUID id;

    @TenantId   // Hibernate filters every query by this column and fills it on insert
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled;

    // Was a @ManyToMany to RoleJpaEntity. A join table with a column of its own (org_unit_id) needs more:
    // each row becomes an embeddable value that holds the role and the unit.
    @ElementCollection
    @CollectionTable(name = "iam_user_role", joinColumns = @JoinColumn(name = "user_id"))
    private Set<RoleGrantEmbeddable> grants = new HashSet<>();

    protected UserJpaEntity() {
        // JPA needs a no-argument constructor
    }

    UserJpaEntity(UUID id, UUID tenantId, String email, String passwordHash, boolean enabled, Set<RoleGrantEmbeddable> grants) {
        this.id = id;
        this.tenantId = tenantId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.grants = grants;
    }

    UUID getId() { return id; }
    UUID getTenantId() { return tenantId; }
    String getEmail() { return email; }
    String getPasswordHash() { return passwordHash; }
    boolean isEnabled() { return enabled; }
    Set<RoleGrantEmbeddable> getGrants() { return grants; }
}
