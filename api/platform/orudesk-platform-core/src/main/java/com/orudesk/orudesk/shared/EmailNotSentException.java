package com.orudesk.orudesk.shared;

/** The email didn't go out. Thrown in a listener, it leaves the event publication incomplete, to be retried. */
public class EmailNotSentException extends RuntimeException {

    public EmailNotSentException(String message, Throwable cause) {
        super(message, cause);
    }
}
