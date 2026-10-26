package com.orudesk.orudesk.identity.domain;

import java.util.Objects;
import java.util.UUID;

/** The unique id of an organisation unit. */
public record OrgUnitId(UUID value) {

    public OrgUnitId {
        Objects.requireNonNull(value, "org unit id must not be null");
    }

    public static OrgUnitId newId() {
        return new OrgUnitId(UUID.randomUUID());
    }
}
