package com.orudesk.orudesk.identity.domain;

import java.util.Objects;

/**
 * A hashed password. The domain never sees the plain password after registration,
 * and toString() never prints the hash.
 */
public record PasswordHash(String value) {

    public PasswordHash {
        Objects.requireNonNull(value, "password hash must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("password hash must not be blank");
        }
    }

    @Override
    public String toString() {
        return "PasswordHash[****]";
    }
}
