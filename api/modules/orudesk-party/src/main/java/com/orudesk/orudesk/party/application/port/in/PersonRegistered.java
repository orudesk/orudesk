package com.orudesk.orudesk.party.application.port.in;

import com.orudesk.orudesk.shared.AuditableEvent;

import java.util.UUID;

/**
 * A person was added to the registry. Part of party's public API: a vertical may listen too (a school creating a
 * student record when a guardian registers a child, say).
 */
public record PersonRegistered(UUID personId, String fullName) implements AuditableEvent {

    @Override public String action() { return "party.person.registered"; }
    @Override public String subjectType() { return "person"; }
    @Override public UUID subjectId() { return personId; }
    @Override public String summary() { return "Registered " + fullName; }
}
