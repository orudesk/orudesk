package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.out.RefreshTokenStore;
import com.orudesk.orudesk.identity.domain.UserId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** A fake Redis for tests. */
class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private final Map<String, UserId> tokens = new HashMap<>();

    @Override
    public String create(UserId userId) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, userId);
        return token;
    }

    @Override
    public Optional<UserId> consume(String refreshToken) {
        return Optional.ofNullable(tokens.remove(refreshToken));
    }

    @Override
    public void revoke(String refreshToken) {
        tokens.remove(refreshToken);
    }

    int size() {
        return tokens.size();
    }
}
