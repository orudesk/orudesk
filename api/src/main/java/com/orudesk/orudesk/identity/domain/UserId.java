package com.orudesk.orudesk.identity.domain;

import java.util.Objects;
import java.util.UUID;

/** The unique id of a user. A type of its own, so it can't be mixed up with a TenantId. */
public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "user id must not be null");
    }

    public static UserId newId() {
        return new UserId(UUID.randomUUID());
    }
}
