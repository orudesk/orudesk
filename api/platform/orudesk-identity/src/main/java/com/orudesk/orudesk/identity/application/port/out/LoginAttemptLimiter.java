package com.orudesk.orudesk.identity.application.port.out;

import com.orudesk.orudesk.identity.domain.Email;

import java.time.Duration;
import java.util.Optional;

/**
 * Counts failed logins per email, so a password can't be guessed by trying thousands. How many failures, over
 * how long, is the adapter's configuration; the login use case only asks and reports.
 */
public interface LoginAttemptLimiter {

    /** Empty if this email may try to log in now; otherwise how long until it may. */
    Optional<Duration> blockedFor(Email email);

    void failed(Email email);

    /** A successful login starts the count again. */
    void succeeded(Email email);
}
