package com.orudesk.orudesk.identity.domain;

/** Login failed. Deliberately doesn't say whether the email or the password was wrong. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("invalid email or password");
    }
}
