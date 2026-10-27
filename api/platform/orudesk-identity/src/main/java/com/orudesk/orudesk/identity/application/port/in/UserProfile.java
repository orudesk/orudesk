package com.orudesk.orudesk.identity.application.port.in;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * A read-only view of a user, safe to show and to cache: no password hash.
 *
 * @param roles  the tenant-wide roles (the same as in the access token)
 * @param grants every grant, tenant-wide (orgUnitId null) and scoped: what the web app's unit switcher shows
 */
public record UserProfile(UUID id, UUID tenantId, String email, List<String> roles, List<Grant> grants, boolean enabled) {

    public record Grant(String role, @Nullable UUID orgUnitId) {
    }
}
