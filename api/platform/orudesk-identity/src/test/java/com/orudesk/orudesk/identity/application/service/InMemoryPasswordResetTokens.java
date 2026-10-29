package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.out.PasswordResetTokens;
import com.orudesk.orudesk.identity.domain.UserId;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** A fake token table for unit tests. Tokens never expire here; the real expiry is tested against PostgreSQL. */
class InMemoryPasswordResetTokens implements PasswordResetTokens {

    private final Map<String, UserId> tokens = new HashMap<>();

    @Override
    public String create(UserId userId) {
        tokens.values().removeIf(userId::equals);
        String token = UUID.randomUUID().toString();
        tokens.put(token, userId);
        return token;
    }

    @Override
    public Optional<UserId> consume(String rawToken, Instant now) {
        return Optional.ofNullable(tokens.remove(rawToken));
    }

    @Override
    public int deleteExpiredBefore(Instant cutoff) {
        return 0;
    }
}
