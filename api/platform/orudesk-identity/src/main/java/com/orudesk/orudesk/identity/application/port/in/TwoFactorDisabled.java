package com.orudesk.orudesk.identity.application.port.in;

import com.orudesk.orudesk.shared.AuditableEvent;

import java.util.UUID;

/** Day 67: a change to how a user signs in is always audited. */
public record TwoFactorDisabled(UUID tenantId, UUID userId, String email) implements AuditableEvent {

    @Override public String action() { return "identity.two-factor.disabled"; }
    @Override public String subjectType() { return "user"; }
    @Override public UUID subjectId() { return userId; }
    @Override public String summary() { return "Turned off two-factor sign-in for " + email; }
}
