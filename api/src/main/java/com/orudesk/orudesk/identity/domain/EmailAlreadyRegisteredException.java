package com.orudesk.orudesk.identity.domain;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(Email email) {
        super("email already registered: " + email.value());
    }
}
