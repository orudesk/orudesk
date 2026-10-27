package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.out.LoginAttemptLimiter;
import com.orudesk.orudesk.identity.domain.Email;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** A fake limiter for unit tests: blocks for 15 minutes from the third failure. */
class InMemoryLoginAttemptLimiter implements LoginAttemptLimiter {

    static final int MAX_FAILURES = 3;

    private final Map<Email, Integer> failures = new HashMap<>();

    @Override
    public Optional<Duration> blockedFor(Email email) {
        return failures.getOrDefault(email, 0) >= MAX_FAILURES ? Optional.of(Duration.ofMinutes(15)) : Optional.empty();
    }

    @Override
    public void failed(Email email) {
        failures.merge(email, 1, Integer::sum);
    }

    @Override
    public void succeeded(Email email) {
        failures.remove(email);
    }

    int failures(Email email) {
        return failures.getOrDefault(email, 0);
    }
}
