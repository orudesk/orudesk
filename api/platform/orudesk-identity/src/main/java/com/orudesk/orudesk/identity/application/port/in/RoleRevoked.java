package com.orudesk.orudesk.identity.application.port.in;

import com.orudesk.orudesk.shared.AuditableEvent;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/** A user lost a role they had. Not published when they didn't have it (the DELETE did nothing). */
public record RoleRevoked(UUID tenantId, UUID userId, String email, String role, @Nullable String unitName)
        implements AuditableEvent {

    @Override public String action() { return "identity.role.revoked"; }
    @Override public String subjectType() { return "user"; }
    @Override public UUID subjectId() { return userId; }
    @Override public String summary() { return "Took " + role + RoleGranted.where(unitName) + " from " + email; }
}
