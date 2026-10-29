package com.orudesk.orudesk.party.application.port.in;

import com.orudesk.orudesk.shared.AuditableEvent;
import com.orudesk.orudesk.shared.FieldChange;

import java.util.List;
import java.util.UUID;

/**
 * A person's details were changed. Published only when something actually changed: saving an untouched form
 * isn't news.
 *
 * @param changes each changed field with its old and new value, in the order of the form (Day 47)
 */
public record PersonEdited(UUID personId, String fullName, List<FieldChange> changes) implements AuditableEvent {

    public PersonEdited {
        changes = List.copyOf(changes);
    }

    @Override public String action() { return "party.person.edited"; }
    @Override public String subjectType() { return "person"; }
    @Override public UUID subjectId() { return personId; }

    @Override
    public String summary() {
        return "Changed " + String.join(", ", changes.stream().map(FieldChange::field).toList()) + " of " + fullName;
    }
}
