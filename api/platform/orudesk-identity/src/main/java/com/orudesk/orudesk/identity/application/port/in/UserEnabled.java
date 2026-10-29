package com.orudesk.orudesk.identity.application.port.in;

import com.orudesk.orudesk.shared.AuditableEvent;

import java.util.UUID;

/** A disabled user may log in again. */
public record UserEnabled(UUID tenantId, UUID userId, String email) implements AuditableEvent {

    @Override public String action() { return "identity.user.enabled"; }
    @Override public String subjectType() { return "user"; }
    @Override public UUID subjectId() { return userId; }
    @Override public String summary() { return "Enabled the login of " + email; }
}
