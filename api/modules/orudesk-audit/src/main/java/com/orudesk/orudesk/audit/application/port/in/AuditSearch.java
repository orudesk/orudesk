package com.orudesk.orudesk.audit.application.port.in;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Which entries, newest first.
 *
 * @param subjectId only what happened to this user, person or unit; null for everything
 * @param after     the nextCursor of the previous page, or null for the first page
 * @param size      entries per page, 1 to 100
 */
public record AuditSearch(@Nullable UUID subjectId, @Nullable String after, int size) {

    public static final int MAX_PAGE_SIZE = 100;

    public AuditSearch {
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }
}
