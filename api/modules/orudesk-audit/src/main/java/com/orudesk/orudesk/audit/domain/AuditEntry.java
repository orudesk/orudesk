package com.orudesk.orudesk.audit.domain;

import com.orudesk.orudesk.shared.FieldChange;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * One thing that happened, as it was known at the time. Never changed after it is written: if the summary was
 * wrong, a later entry says so.
 *
 * @param actorId    who did it; the nil UUID for the system (a startup seed), null when nobody was logged in
 * @param actorEmail their email at the time, so the entry still reads right if they change it or leave
 * @param requestId  the request it happened in (Day 41), to find its log lines
 * @param changes    for an edit, the fields it changed (Day 47); empty otherwise
 */
public record AuditEntry(UUID id, UUID tenantId, Instant occurredAt, @Nullable UUID actorId,
                         @Nullable String actorEmail, String action, String subjectType, UUID subjectId,
                         String summary, @Nullable String requestId, List<FieldChange> changes) {

    public AuditEntry {
        Objects.requireNonNull(id);
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(subjectId);
        if (!action.matches("[a-z][a-z0-9.-]{2,63}")) {
            throw new IllegalArgumentException("action must look like module.subject.verb, was: " + action);
        }
        if (subjectType.isBlank() || subjectType.length() > 32) {
            throw new IllegalArgumentException("subject type must be 1 to 32 characters");
        }
        // Summaries are for people and short; a long one is cut rather than refused, so auditing never
        // blocks the change it records
        summary = summary.length() > 500 ? summary.substring(0, 499) + "…" : summary;
        changes = List.copyOf(changes);
    }
}
