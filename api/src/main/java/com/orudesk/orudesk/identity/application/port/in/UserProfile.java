package com.orudesk.orudesk.identity.application.port.in;

import java.util.List;
import java.util.UUID;

/** A read-only view of a user, safe to show and to cache: no password hash. */
public record UserProfile(UUID id, UUID tenantId, String email, List<String> roles, boolean enabled) {
}
