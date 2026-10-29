package com.orudesk.orudesk.identity.application.port.in;

import com.orudesk.orudesk.shared.AuditableEvent;

import java.util.UUID;

/** A user can no longer log in. Not published when they already couldn't. */
public record UserDisabled(UUID tenantId, UUID userId, String email) implements AuditableEvent {

    @Override public String action() { return "identity.user.disabled"; }
    @Override public String subjectType() { return "user"; }
    @Override public UUID subjectId() { return userId; }
    @Override public String summary() { return "Disabled the login of " + email; }
}
