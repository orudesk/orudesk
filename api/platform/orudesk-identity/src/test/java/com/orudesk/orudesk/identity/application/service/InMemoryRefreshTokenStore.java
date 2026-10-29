package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.ClientInfo;
import com.orudesk.orudesk.identity.application.port.out.RefreshGrant;
import com.orudesk.orudesk.identity.application.port.out.RefreshTokenStore;
import com.orudesk.orudesk.identity.application.port.out.SessionDetails;
import com.orudesk.orudesk.identity.application.port.out.SessionRef;
import com.orudesk.orudesk.identity.domain.UserId;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** A fake Redis for tests: tokens point at sessions, as in RedisRefreshTokenStore. */
class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private final Map<String, SessionRef> tokens = new HashMap<>();
    private final Map<UUID, SessionDetails> sessions = new LinkedHashMap<>();
    private final Map<UUID, UserId> owners = new HashMap<>();

    @Override
    public RefreshGrant start(UserId userId, ClientInfo client) {
        SessionRef session = new SessionRef(userId, UUID.randomUUID());
        return new RefreshGrant(renew(session, client), session.sessionId());
    }

    @Override
    public Optional<SessionRef> consume(String refreshToken) {
        return Optional.ofNullable(tokens.remove(refreshToken));
    }

    @Override
    public String renew(SessionRef session, ClientInfo client) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, session);
        Instant now = Instant.now();
        SessionDetails before = sessions.get(session.sessionId());
        sessions.put(session.sessionId(), new SessionDetails(session.sessionId(),
                before == null ? now : before.startedAt(), now, client.device(), client.ipAddress()));
        owners.put(session.sessionId(), session.userId());
        return token;
    }

    @Override
    public void revoke(String refreshToken) {
        consume(refreshToken).ifPresent(s -> end(s.userId(), s.sessionId()));
    }

    @Override
    public List<SessionDetails> sessions(UserId userId) {
        List<SessionDetails> mine = new ArrayList<>();
        sessions.values().stream().filter(s -> userId.equals(owners.get(s.id()))).forEach(mine::add);
        return mine;
    }

    @Override
    public boolean end(UserId userId, UUID sessionId) {
        if (!userId.equals(owners.get(sessionId))) {
            return false;
        }
        sessions.remove(sessionId);
        owners.remove(sessionId);
        tokens.values().removeIf(s -> s.sessionId().equals(sessionId));
        return true;
    }

    @Override
    public int endAllExcept(UserId userId, @Nullable UUID keep) {
        int ended = 0;
        for (SessionDetails s : sessions(userId)) {
            if (!s.id().equals(keep) && end(userId, s.id())) {
                ended++;
            }
        }
        return ended;
    }

    @Override
    public boolean isActive(UUID sessionId) {
        return sessions.containsKey(sessionId);
    }

    int size() {
        return tokens.size();
    }
}
