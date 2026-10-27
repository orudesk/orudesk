package com.orudesk.orudesk.identity.application.port.in;

import com.orudesk.orudesk.shared.AuditableEvent;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/** A user got a role, for the whole tenant (no unit) or at one org unit. */
public record RoleGranted(UUID tenantId, UUID userId, String email, String role, @Nullable String unitName)
        implements AuditableEvent {

    @Override public String action() { return "identity.role.granted"; }
    @Override public String subjectType() { return "user"; }
    @Override public UUID subjectId() { return userId; }
    @Override public String summary() { return "Gave " + role + where(unitName) + " to " + email; }

    static String where(@Nullable String unitName) {
        return unitName == null ? " in the whole organisation" : " in " + unitName;
    }
}
