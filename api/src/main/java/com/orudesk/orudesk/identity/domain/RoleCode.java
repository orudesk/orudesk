package com.orudesk.orudesk.identity.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** The code of a role, such as ADMIN or USER. Always upper case. */
public record RoleCode(String value) {

    // Must come first: the constants below run the constructor, which uses FORMAT.
    private static final Pattern FORMAT = Pattern.compile("^[A-Z][A-Z0-9_]{1,49}$");

    public static final RoleCode ADMIN = new RoleCode("ADMIN");
    public static final RoleCode USER = new RoleCode("USER");

    public RoleCode {
        Objects.requireNonNull(value, "role code must not be null");
        value = value.trim().toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid role code: " + value);
        }
    }
}
