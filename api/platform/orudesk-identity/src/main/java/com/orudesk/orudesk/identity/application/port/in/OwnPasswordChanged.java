package com.orudesk.orudesk.identity.application.port.in;

import com.orudesk.orudesk.shared.AuditableEvent;

import java.util.UUID;

/** A user changed their own password, knowing the old one (Day 69). Same action as a reset, different story. */
public record OwnPasswordChanged(UUID tenantId, UUID userId, String email, int sessionsEnded) implements AuditableEvent {

    @Override public String action() { return "identity.user.password-changed"; }
    @Override public String subjectType() { return "user"; }
    @Override public UUID subjectId() { return userId; }
    @Override public String summary() {
        return email + " changed their password; signed out of " + sessionsEnded + " other session"
                + (sessionsEnded == 1 ? "" : "s");
    }
}
