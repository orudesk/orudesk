package com.orudesk.orudesk.identity.domain;

public class UnknownRoleException extends RuntimeException {

    public UnknownRoleException(RoleCode role) {
        super("unknown role: " + role.value());
    }
}
