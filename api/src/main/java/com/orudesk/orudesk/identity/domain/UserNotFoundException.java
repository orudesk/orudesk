package com.orudesk.orudesk.identity.domain;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UserId id) {
        super("user not found: " + id.value());
    }
}
