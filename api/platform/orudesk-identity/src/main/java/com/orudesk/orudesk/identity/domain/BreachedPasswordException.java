package com.orudesk.orudesk.identity.domain;

/**
 * The new password appears in known data breaches (Day 69).
 *
 * @param field the input that holds it ("password" when registering, "newPassword" when changing), so the form can
 *              show the message under it
 */
public class BreachedPasswordException extends RuntimeException {

    private final String field;

    public BreachedPasswordException(String field) {
        super("this password has appeared in a data breach");
        this.field = field;
    }

    public String field() {
        return field;
    }
}
