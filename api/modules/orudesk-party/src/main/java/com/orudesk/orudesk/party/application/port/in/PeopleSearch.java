package com.orudesk.orudesk.party.application.port.in;

/**
 * What to search for.
 *
 * @param text  part of a name, phone or email; null or blank lists everyone
 * @param after the nextCursor of the previous page, or null for the first page
 * @param size  how many people per page, 1 to {@link SearchPeopleQuery#MAX_PAGE_SIZE}
 */
public record PeopleSearch(String text, String after, int size) {

    public PeopleSearch {
        if (size < 1 || size > SearchPeopleQuery.MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + SearchPeopleQuery.MAX_PAGE_SIZE);
        }
        text = text == null || text.isBlank() ? null : text.trim();
        if (text != null && text.length() < 3) {
            throw new IllegalArgumentException("search text must be at least 3 characters");
        }
    }
}
