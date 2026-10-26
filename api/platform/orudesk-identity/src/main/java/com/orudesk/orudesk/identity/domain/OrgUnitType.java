package com.orudesk.orudesk.identity.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * What kind of unit it is: ORGANISATION, GROUP, SCHOOL, HOSPITAL, BRANCH...
 * A code, not an enum, so a vertical can bring its own types (a CAMPUS, a WARD) without changing the core.
 */
public record OrgUnitType(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Z][A-Z0-9_]{1,49}$");

    public OrgUnitType {
        Objects.requireNonNull(value, "org unit type must not be null");
        value = value.trim().toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid org unit type: " + value);
        }
    }
}
