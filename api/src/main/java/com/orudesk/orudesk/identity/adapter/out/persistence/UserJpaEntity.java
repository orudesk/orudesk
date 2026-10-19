package com.orudesk.orudesk.identity.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** One row of iam_user, plus its roles through the iam_user_role join table. */
@Entity
@Table(name = "iam_user")
class UserJpaEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled;

    @ManyToMany
    @JoinTable(
            name = "iam_user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<RoleJpaEntity> roles = new HashSet<>();

    protected UserJpaEntity() {
        // JPA needs a no-argument constructor
    }

    UserJpaEntity(UUID id, UUID tenantId, String email, String passwordHash, boolean enabled, Set<RoleJpaEntity> roles) {
        this.id = id;
        this.tenantId = tenantId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.roles = roles;
    }

    UUID getId() { return id; }
    UUID getTenantId() { return tenantId; }
    String getEmail() { return email; }
    String getPasswordHash() { return passwordHash; }
    boolean isEnabled() { return enabled; }
    Set<RoleJpaEntity> getRoles() { return roles; }
}
