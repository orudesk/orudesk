package com.orudesk.orudesk.identity.application.port.in;

import com.orudesk.orudesk.shared.AuditableEvent;

import java.util.UUID;

/** A user signed some of their sessions out (Day 66): worth a line in the audit log, as any security change. */
public record SessionsEnded(UUID tenantId, UUID userId, String email, int count) implements AuditableEvent {

    @Override public String action() { return "identity.sessions.ended"; }
    @Override public String subjectType() { return "user"; }
    @Override public UUID subjectId() { return userId; }
    @Override public String summary() {
        return count == 1 ? "Signed " + email + " out of 1 session" : "Signed " + email + " out of " + count + " sessions";
    }
}
