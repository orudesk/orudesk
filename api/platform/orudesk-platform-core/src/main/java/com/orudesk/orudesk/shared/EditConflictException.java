package com.orudesk.orudesk.shared;

import java.util.UUID;

/**
 * Someone saved a change to a record that was changed after they read it: they edited version 3, but it is at
 * version 4 now. Saving anyway would silently undo the other person's change (a "lost update"), so the edit is
 * refused and the client is told to reload. Every module's edits raise this one, so the API answers them all the
 * same way: 409 Conflict.
 *
 * Here in the top package of "shared", like AuditableEvent: any module may use it.
 */
public class EditConflictException extends RuntimeException {

    private final String subjectType;
    private final UUID subjectId;
    private final long currentVersion;

    public EditConflictException(String subjectType, UUID subjectId, long expectedVersion, long currentVersion) {
        super("this " + subjectType + " was changed by someone else after you opened it (you had version "
                + expectedVersion + ", it is now at " + currentVersion + "); reload it and make your change again");
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.currentVersion = currentVersion;
    }

    public String subjectType() { return subjectType; }
    public UUID subjectId() { return subjectId; }
    public long currentVersion() { return currentVersion; }
}
