package com.orudesk.orudesk.identity.domain;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A person who can log in. It protects its own rules:
 * a user always has at least one role, and a disabled user can't be given new roles.
 */
public class User {

    private final UserId id;
    private final TenantId tenantId;
    private final Email email;
    private final PasswordHash passwordHash;
    private boolean enabled;
    private final Set<RoleCode> roles;

    private User(UserId id, TenantId tenantId, Email email, PasswordHash passwordHash,
                 boolean enabled, Set<RoleCode> roles) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        this.enabled = enabled;
        Objects.requireNonNull(roles, "roles must not be null");
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("a user must have at least one role");
        }
        this.roles = new HashSet<>(roles);
    }

    /** A brand-new user: a fresh id, enabled, with the default USER role. */
    public static User register(TenantId tenantId, Email email, PasswordHash passwordHash) {
        return new User(UserId.newId(), tenantId, email, passwordHash, true, Set.of(RoleCode.USER));
    }

    /** Rebuilds a user that already exists, for example after loading it from the database. */
    public static User restore(UserId id, TenantId tenantId, Email email, PasswordHash passwordHash,
                               boolean enabled, Set<RoleCode> roles) {
        return new User(id, tenantId, email, passwordHash, enabled, roles);
    }

    public void assignRole(RoleCode role) {
        Objects.requireNonNull(role, "role must not be null");
        if (!enabled) {
            throw new IllegalStateException("cannot assign a role to a disabled user");
        }
        roles.add(role);
    }

    public void removeRole(RoleCode role) {
        if (roles.contains(role) && roles.size() == 1) {
            throw new IllegalStateException("a user must have at least one role");
        }
        roles.remove(role);
    }

    public void disable() {
        enabled = false;
    }

    public boolean hasRole(RoleCode role) {
        return roles.contains(role);
    }

    public UserId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public Email email() { return email; }
    public PasswordHash passwordHash() { return passwordHash; }
    public boolean isEnabled() { return enabled; }

    /** A read-only copy, so callers can't change the roles without going through the rules above. */
    public Set<RoleCode> roles() { return Set.copyOf(roles); }
}
