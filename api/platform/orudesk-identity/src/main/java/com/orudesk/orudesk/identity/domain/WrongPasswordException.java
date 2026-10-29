package com.orudesk.orudesk.identity.domain;

/** Asked again before a security change (Day 67): someone at an unlocked computer can't change it alone. */
public class WrongPasswordException extends RuntimeException {

    public WrongPasswordException() {
        super("the current password is not right");
    }
}
