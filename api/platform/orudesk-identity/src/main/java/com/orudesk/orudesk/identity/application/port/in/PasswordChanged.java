package com.orudesk.orudesk.identity.application.port.in;

import com.orudesk.orudesk.shared.AuditableEvent;

import java.util.UUID;

/** A user set a new password with a reset link (Day 53). Audited: "who changed my password?" must have an answer. */
public record PasswordChanged(UUID tenantId, UUID userId, String email) implements AuditableEvent {

    @Override public String action() { return "identity.user.password-changed"; }
    @Override public String subjectType() { return "user"; }
    @Override public UUID subjectId() { return userId; }
    @Override public String summary() { return "Reset the password of " + email; }
}
