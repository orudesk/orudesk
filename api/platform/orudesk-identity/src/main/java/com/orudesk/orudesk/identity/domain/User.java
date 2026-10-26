package com.orudesk.orudesk.identity.domain;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A person who can log in. It protects its own rules:
 * a user always has at least one role, and a disabled user can't be given new roles.
 * Roles are held as grants: tenant-wide, or scoped to one org unit (Day 19).
 */
public class User {

    private final UserId id;
    private final TenantId tenantId;
    private final Email email;
    private final PasswordHash passwordHash;
    private boolean enabled;
    private final Set<RoleGrant> grants;

    private User(UserId id, TenantId tenantId, Email email, PasswordHash passwordHash,
                 boolean enabled, Set<RoleGrant> grants) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        this.enabled = enabled;
        Objects.requireNonNull(grants, "grants must not be null");
        if (grants.isEmpty()) {
            throw new IllegalArgumentException("a user must have at least one role");
        }
        this.grants = new HashSet<>(grants);
    }

    /** A brand-new user: a fresh id, enabled, with the default USER role. */
    public static User register(TenantId tenantId, Email email, PasswordHash passwordHash) {
        return new User(UserId.newId(), tenantId, email, passwordHash, true, Set.of(RoleGrant.tenantWide(RoleCode.USER)));
    }

    /** Rebuilds a user that already exists, for example after loading it from the database. */
    public static User restore(UserId id, TenantId tenantId, Email email, PasswordHash passwordHash,
                               boolean enabled, Set<RoleGrant> grants) {
        return new User(id, tenantId, email, passwordHash, enabled, grants);
    }

    /** The role everywhere in the tenant. */
    public void assignRole(RoleCode role) {
        grant(RoleGrant.tenantWide(role));
    }

    /** The role in one org unit and everything below it. */
    public void assignRoleAt(RoleCode role, OrgUnitId unit) {
        grant(RoleGrant.at(role, unit));
    }

    private void grant(RoleGrant grant) {
        if (!enabled) {
            throw new IllegalStateException("cannot assign a role to a disabled user");
        }
        grants.add(grant);
    }

    /** Removes the tenant-wide grant of this role. */
    public void removeRole(RoleCode role) {
        RoleGrant grant = RoleGrant.tenantWide(role);
        if (grants.contains(grant) && grants.size() == 1) {
            throw new IllegalStateException("a user must have at least one role");
        }
        grants.remove(grant);
    }

    public void disable() {
        enabled = false;
    }

    /** True if the user has the role everywhere in the tenant. Scoped roles are checked against the org tree. */
    public boolean hasRole(RoleCode role) {
        return grants.contains(RoleGrant.tenantWide(role));
    }

    public UserId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public Email email() { return email; }
    public PasswordHash passwordHash() { return passwordHash; }
    public boolean isEnabled() { return enabled; }

    /** The tenant-wide roles: what goes into the access token. A read-only copy, like grants(). */
    public Set<RoleCode> roles() {
        return grants.stream().filter(RoleGrant::isTenantWide).map(RoleGrant::role).collect(Collectors.toUnmodifiableSet());
    }

    /** Every grant, tenant-wide and scoped. A read-only copy, so callers can't skip the rules above. */
    public Set<RoleGrant> grants() { return Set.copyOf(grants); }
}
