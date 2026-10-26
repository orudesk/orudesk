package com.orudesk.orudesk.identity.application.port.in;

import com.orudesk.orudesk.shared.AuditableEvent;

import java.util.UUID;

/** A login was created, by someone signing up or by an admin. */
public record UserRegistered(UUID tenantId, UUID userId, String email) implements AuditableEvent {

    @Override public String action() { return "identity.user.registered"; }
    @Override public String subjectType() { return "user"; }
    @Override public UUID subjectId() { return userId; }
    @Override public String summary() { return "Registered " + email; }
}
