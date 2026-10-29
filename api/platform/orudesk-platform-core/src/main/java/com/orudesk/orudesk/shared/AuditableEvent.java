package com.orudesk.orudesk.shared;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Something that happened which an admin may later ask about: "who gave Asha the ADMIN role, and when?". A module
 * publishes it with Spring's ApplicationEventPublisher after making the change; the audit module records it in
 * the same transaction, with who did it and the request id. Modules don't know the audit module exists: they
 * only describe what happened. A vertical's events become auditable by implementing this interface too.
 *
 * Here in the top package of "shared", because that is the part every module may use (see ModuleMigrations).
 */
public interface AuditableEvent {

    /** What happened, as module.subject.verb: "identity.role.granted". Stable: reports filter by it. */
    String action();

    /** What kind of thing it happened to: "user", "person", "org-unit". */
    String subjectType();

    /** Which one. Kept as an id, so the entry stays valid if the thing is renamed or deleted later. */
    UUID subjectId();

    /** One line for people, written at the time: "Gave ADMIN in North School to asha@example.org". */
    String summary();

    /**
     * The tenant it happened in, when the event knows it. Needed when nobody is logged in (someone signing up,
     * the first admin at startup); otherwise the logged-in user's tenant is used.
     */
    default @Nullable UUID tenantId() {
        return null;
    }

    /**
     * For an edit: which fields changed, from what to what (Day 47). Empty for events that aren't edits (a
     * registration, a role given), where the summary says it all.
     */
    default List<FieldChange> changes() {
        return List.of();
    }
}
