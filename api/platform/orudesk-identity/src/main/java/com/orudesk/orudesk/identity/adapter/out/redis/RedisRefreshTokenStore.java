package com.orudesk.orudesk.identity.adapter.out.redis;

import com.orudesk.orudesk.identity.application.port.in.ClientInfo;
import com.orudesk.orudesk.identity.application.port.out.RefreshGrant;
import com.orudesk.orudesk.identity.application.port.out.RefreshTokenStore;
import com.orudesk.orudesk.identity.application.port.out.SessionDetails;
import com.orudesk.orudesk.identity.application.port.out.SessionRef;
import com.orudesk.orudesk.identity.domain.UserId;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps refresh tokens and sessions in Redis. Only a hash of a token is stored, so someone who reads Redis can't
 * use the tokens. Every key expires by itself after the refresh token lifetime, counted from the session's last use.
 *
 * <pre>
 * orudesk:refresh:{sha256 of token}   → "{user id}:{session id}"
 * orudesk:session:{session id}        → hash: user, started, used, device, ip, token (the current token's key)
 * orudesk:sessions-by-user:{user id}  → set of session ids
 * </pre>
 *
 * Day 66 added the sessions. A token from before then holds a bare user id: it still works once, and its refresh
 * starts a session for it. The Day 53 set "orudesk:refresh-by-user:{user id}" is still emptied by
 * {@link #endAllExcept}, for those older tokens.
 */
@Component
class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "orudesk:refresh:";
    private static final String SESSION_PREFIX = "orudesk:session:";
    private static final String SESSIONS_BY_USER_PREFIX = "orudesk:sessions-by-user:";
    private static final String OLD_BY_USER_PREFIX = "orudesk:refresh-by-user:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;
    private final Duration ttl;
    private final Clock clock;

    RedisRefreshTokenStore(StringRedisTemplate redis,
                           @Value("${orudesk.security.jwt.refresh-token-ttl}") Duration ttl, Clock clock) {
        this.redis = redis;
        this.ttl = ttl;
        this.clock = clock;
    }

    @Override
    public RefreshGrant start(UserId userId, ClientInfo client) {
        SessionRef session = new SessionRef(userId, UUID.randomUUID());
        return new RefreshGrant(issue(session, client, true), session.sessionId());
    }

    @Override
    public Optional<SessionRef> consume(String refreshToken) {
        // GETDEL reads and deletes in one atomic step, so two requests can't both use the same token.
        String value = redis.opsForValue().getAndDelete(key(refreshToken));
        if (value == null) {
            return Optional.empty();
        }
        int colon = value.indexOf(':');
        if (colon < 0) {   // a token from before Day 66: its refresh starts a session
            return Optional.of(new SessionRef(new UserId(UUID.fromString(value)), UUID.randomUUID()));
        }
        return Optional.of(new SessionRef(new UserId(UUID.fromString(value.substring(0, colon))),
                UUID.fromString(value.substring(colon + 1))));
    }

    @Override
    public String renew(SessionRef session, ClientInfo client) {
        return issue(session, client, !Boolean.TRUE.equals(redis.hasKey(sessionKey(session.sessionId()))));
    }

    @Override
    public void revoke(String refreshToken) {
        consume(refreshToken).ifPresent(session -> end(session.userId(), session.sessionId()));
    }

    @Override
    public List<SessionDetails> sessions(UserId userId) {
        List<SessionDetails> sessions = new ArrayList<>();
        for (String id : members(userId)) {
            Map<Object, Object> fields = redis.opsForHash().entries(sessionKey(UUID.fromString(id)));
            if (fields.isEmpty()) {   // expired by itself: tidy the set
                redis.opsForSet().remove(byUser(userId), id);
                continue;
            }
            sessions.add(new SessionDetails(UUID.fromString(id),
                    Instant.parse((String) fields.get("started")), Instant.parse((String) fields.get("used")),
                    (String) fields.get("device"), (String) fields.get("ip")));
        }
        sessions.sort(Comparator.comparing(SessionDetails::lastUsedAt).reversed());
        return sessions;
    }

    @Override
    public boolean end(UserId userId, UUID sessionId) {
        if (!Boolean.TRUE.equals(redis.opsForSet().isMember(byUser(userId), sessionId.toString()))) {
            return false;   // not theirs, or long gone
        }
        Object tokenKey = redis.opsForHash().get(sessionKey(sessionId), "token");
        if (tokenKey != null) {
            redis.delete((String) tokenKey);
        }
        redis.delete(sessionKey(sessionId));
        redis.opsForSet().remove(byUser(userId), sessionId.toString());
        return true;
    }

    @Override
    public int endAllExcept(UserId userId, @Nullable UUID keep) {
        int ended = 0;
        for (String id : members(userId)) {
            UUID sessionId = UUID.fromString(id);
            if (!sessionId.equals(keep) && end(userId, sessionId)) {
                ended++;
            }
        }
        String oldByUser = OLD_BY_USER_PREFIX + userId.value();
        Set<String> oldKeys = redis.opsForSet().members(oldByUser);
        if (oldKeys != null && !oldKeys.isEmpty()) {
            redis.delete(oldKeys);
        }
        redis.delete(oldByUser);
        return ended;
    }

    @Override
    public boolean isActive(UUID sessionId) {
        return Boolean.TRUE.equals(redis.hasKey(sessionKey(sessionId)));
    }

    /** A new token for the session, recorded as its current one; every key's lifetime starts again. */
    private String issue(SessionRef session, ClientInfo client, boolean starting) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String tokenKey = key(token);
        String now = Instant.now(clock).toString();
        String sessionKey = sessionKey(session.sessionId());

        redis.opsForValue().set(tokenKey, session.userId().value() + ":" + session.sessionId(), ttl);
        if (starting) {
            redis.opsForHash().putAll(sessionKey, Map.of("user", session.userId().value().toString(), "started", now));
        }
        redis.opsForHash().putAll(sessionKey, Map.of("used", now, "device", client.device(),
                "ip", client.ipAddress(), "token", tokenKey));
        redis.expire(sessionKey, ttl);
        redis.opsForSet().add(byUser(session.userId()), session.sessionId().toString());
        redis.expire(byUser(session.userId()), ttl);
        return token;
    }

    private Set<String> members(UserId userId) {
        Set<String> ids = redis.opsForSet().members(byUser(userId));
        return ids == null ? Set.of() : ids;
    }

    private static String sessionKey(UUID sessionId) {
        return SESSION_PREFIX + sessionId;
    }

    private static String byUser(UserId userId) {
        return SESSIONS_BY_USER_PREFIX + userId.value();
    }

    private static String key(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return KEY_PREFIX + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available in Java", e);
        }
    }
}
