package com.orudesk.orudesk.identity.domain;

import java.util.Objects;
import java.util.UUID;

/** The id of the organization (tenant) a user belongs to. */
public record TenantId(UUID value) {

    public TenantId {
        Objects.requireNonNull(value, "tenant id must not be null");
    }
}
