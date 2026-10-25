package com.orudesk.orudesk.identity.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * An email address that is always valid and always lower case.
 * If you have an Email object, you know it is correct.
 */
public record Email(String value) {

    private static final Pattern SIMPLE_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MAX_LENGTH = 320;

    public Email {
        Objects.requireNonNull(value, "email must not be null");
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("email must be at most " + MAX_LENGTH + " characters");
        }
        if (!SIMPLE_EMAIL.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid email: " + value);
        }
    }
}