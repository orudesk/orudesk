package com.orudesk.orudesk.identity.domain;

public class OrgUnitNotFoundException extends RuntimeException {

    public OrgUnitNotFoundException(OrgUnitId id) {
        super("org unit not found: " + id.value());
    }

    public OrgUnitNotFoundException(String message) {
        super(message);
    }
}
