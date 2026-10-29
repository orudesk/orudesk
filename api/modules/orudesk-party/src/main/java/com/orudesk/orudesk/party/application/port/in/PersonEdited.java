package com.orudesk.orudesk.party.application.port.in;

import com.orudesk.orudesk.shared.AuditableEvent;

import java.util.List;
import java.util.UUID;

/**
 * A person's details were changed. Published only when something actually changed: saving an untouched form
 * isn't news.
 *
 * @param changedFields the names of the fields that changed, in the order of the form: ["phone", "email"]
 */
public record PersonEdited(UUID personId, String fullName, List<String> changedFields) implements AuditableEvent {

    public PersonEdited {
        changedFields = List.copyOf(changedFields);
    }

    @Override public String action() { return "party.person.edited"; }
    @Override public String subjectType() { return "person"; }
    @Override public UUID subjectId() { return personId; }
    @Override public String summary() { return "Changed " + String.join(", ", changedFields) + " of " + fullName; }
}
