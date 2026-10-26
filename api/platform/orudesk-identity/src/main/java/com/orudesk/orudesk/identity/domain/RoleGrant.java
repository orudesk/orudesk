package com.orudesk.orudesk.identity.domain;

import java.util.Objects;
import java.util.Optional;

/**
 * A role, and where it applies: the whole tenant (no unit), or one org unit and everything below it.
 * "ADMIN" for the owner, "PRINCIPAL at Sunrise School A" for a principal.
 */
public record RoleGrant(RoleCode role, OrgUnitId scope) {

    public RoleGrant {
        Objects.requireNonNull(role, "role must not be null");
    }

    public static RoleGrant tenantWide(RoleCode role) {
        return new RoleGrant(role, null);
    }

    public static RoleGrant at(RoleCode role, OrgUnitId unit) {
        return new RoleGrant(role, Objects.requireNonNull(unit, "unit must not be null"));
    }

    public boolean isTenantWide() {
        return scope == null;
    }

    public Optional<OrgUnitId> scopeId() {
        return Optional.ofNullable(scope);
    }
}
