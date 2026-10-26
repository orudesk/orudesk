package com.orudesk.orudesk.identity.domain;

import java.time.Duration;

/**
 * Too many failed logins for this email lately: no more tries until `retryAfter` has passed, even with the
 * right password. Says nothing about whether the account exists: unknown emails are limited the same way.
 */
public class TooManyLoginAttemptsException extends RuntimeException {

    private final Duration retryAfter;

    public TooManyLoginAttemptsException(Duration retryAfter) {
        super("too many failed logins, try again later");
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
