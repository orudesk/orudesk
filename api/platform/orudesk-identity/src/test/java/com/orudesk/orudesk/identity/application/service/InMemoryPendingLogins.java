package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.out.PendingLogins;
import com.orudesk.orudesk.identity.domain.UserId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** A fake of the pending logins in Redis, for tests. Nothing expires. */
class InMemoryPendingLogins implements PendingLogins {

    private final Map<String, UserId> users = new HashMap<>();
    private final Map<String, Integer> failures = new HashMap<>();

    @Override
    public String start(UserId userId) {
        String token = UUID.randomUUID().toString();
        users.put(token, userId);
        return token;
    }

    @Override
    public Optional<UserId> find(String token) {
        return Optional.ofNullable(users.get(token));
    }

    @Override
    public void failed(String token) {
        if (users.containsKey(token) && failures.merge(token, 1, Integer::sum) >= MAX_FAILURES) {
            finish(token);
        }
    }

    @Override
    public boolean finish(String token) {
        failures.remove(token);
        return users.remove(token) != null;
    }
}
