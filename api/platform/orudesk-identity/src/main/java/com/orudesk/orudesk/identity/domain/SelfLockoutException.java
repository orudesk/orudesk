package com.orudesk.orudesk.identity.domain;

/** An admin tried to lock themselves out (Day 48): another admin has to do it, so someone can still undo it. */
public class SelfLockoutException extends RuntimeException {

    public SelfLockoutException(String message) {
        super(message);
    }
}
