package com.orudesk.orudesk.party.domain;

/** A person's first and last name, trimmed. Both are required. */
public record PersonName(String first, String last) {

    private static final int MAX_LENGTH = 100;

    public PersonName {
        first = required(first, "first name");
        last = required(last, "last name");
    }

    private static String required(String value, String what) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(what + " must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(what + " must be at most " + MAX_LENGTH + " characters");
        }
        return trimmed;
    }
}
