package com.orudesk.orudesk.identity.domain;

public class OrgUnitCodeTakenException extends RuntimeException {

    public OrgUnitCodeTakenException(String code) {
        super("another org unit already has the code: " + code);
    }
}
