package com.orudesk.orudesk.identity.application.port.in;

import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Which users to list.
 *
 * @param text  part of an email address; null or blank lists everyone
 * @param after the nextCursor of the previous page, or null for the first page
 * @param size  how many users per page, 1 to {@link ListUsersQuery#MAX_PAGE_SIZE}
 */
public record UserSearch(@Nullable String text, @Nullable String after, int size) {

    public UserSearch {
        if (size < 1 || size > ListUsersQuery.MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + ListUsersQuery.MAX_PAGE_SIZE);
        }
        text = text == null || text.isBlank() ? null : text.trim().toLowerCase(Locale.ROOT);
    }
}
