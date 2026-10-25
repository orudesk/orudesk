package com.orudesk.orudesk.party.domain;

import java.util.Objects;
import java.util.UUID;

/** The unique id of a person in the registry. */
public record PersonId(UUID value) {

    public PersonId {
        Objects.requireNonNull(value, "person id must not be null");
    }

    public static PersonId newId() {
        return new PersonId(UUID.randomUUID());
    }
}
