package com.orudesk.orudesk.identity.domain;

/** The reset link is unknown, expired or already used. One message for all three: the caller needs a new link. */
public class InvalidPasswordResetTokenException extends RuntimeException {

    public InvalidPasswordResetTokenException() {
        super("this reset link is invalid or has expired; ask for a new one");
    }
}
