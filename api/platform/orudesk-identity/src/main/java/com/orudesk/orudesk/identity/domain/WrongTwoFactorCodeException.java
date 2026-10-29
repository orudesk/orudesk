package com.orudesk.orudesk.identity.domain;

/** The six digits (or a recovery code) didn't match (Day 67). */
public class WrongTwoFactorCodeException extends RuntimeException {

    public WrongTwoFactorCodeException() {
        super("that code is not right, or was already used");
    }
}
