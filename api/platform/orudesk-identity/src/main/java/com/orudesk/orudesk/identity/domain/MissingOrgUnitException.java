package com.orudesk.orudesk.identity.domain;

public class MissingOrgUnitException extends RuntimeException {

    public MissingOrgUnitException(String header) {
        super("this request needs an org unit: send its id in the " + header + " header");
    }
}
