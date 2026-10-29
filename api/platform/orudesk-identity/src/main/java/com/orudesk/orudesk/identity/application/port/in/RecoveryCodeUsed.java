package com.orudesk.orudesk.identity.application.port.in;

import com.orudesk.orudesk.shared.AuditableEvent;

import java.util.UUID;

/** Day 68: a recovery code usually means a lost phone, worth seeing in the audit log. */
public record RecoveryCodeUsed(UUID tenantId, UUID userId, String email, int codesLeft) implements AuditableEvent {

    @Override public String action() { return "identity.two-factor.recovery-code-used"; }
    @Override public String subjectType() { return "user"; }
    @Override public UUID subjectId() { return userId; }
    @Override public String summary() { return email + " signed in with a recovery code; " + codesLeft + " left"; }
}
