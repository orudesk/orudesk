package com.orudesk.orudesk.identity.adapter.out.redis;

import com.orudesk.orudesk.identity.application.port.out.PendingLogins;
import com.orudesk.orudesk.identity.domain.UserId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Pending logins in Redis (Day 68): key "orudesk:pending-login:{sha256 of token}", a hash with the user and the wrong
 * codes so far, expiring after `ttl`. Only the hash of the token is stored, as for refresh tokens: someone who can
 * read Redis can't use what they read.
 */
@Component
class RedisPendingLogins implements PendingLogins {

    private static final String KEY_PREFIX = "orudesk:pending-login:";
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Count a failure and, at the limit, delete: one step, so two quick guesses can't both slip under it. */
    private static final RedisScript<Long> COUNT_FAILURE = RedisScript.of("""
            if redis.call('EXISTS', KEYS[1]) == 0 then return 0 end
            local failures = redis.call('HINCRBY', KEYS[1], 'failures', 1)
            if failures >= tonumber(ARGV[1]) then redis.call('DEL', KEYS[1]) end
            return failures
            """, Long.class);

    private final StringRedisTemplate redis;
    private final Duration ttl;

    RedisPendingLogins(StringRedisTemplate redis, @Value("${orudesk.security.login.second-step-ttl:5m}") Duration ttl) {
        this.redis = redis;
        this.ttl = ttl;
    }

    @Override
    public String start(UserId userId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        redis.opsForHash().putAll(key(token), Map.of("user", userId.value().toString(), "failures", "0"));
        redis.expire(key(token), ttl);
        return token;
    }

    @Override
    public Optional<UserId> find(String token) {
        Object user = redis.opsForHash().get(key(token), "user");
        return Optional.ofNullable(user).map(u -> new UserId(UUID.fromString(u.toString())));
    }

    @Override
    public void failed(String token) {
        redis.execute(COUNT_FAILURE, List.of(key(token)), Integer.toString(MAX_FAILURES));
    }

    @Override
    public boolean finish(String token) {
        return Boolean.TRUE.equals(redis.delete(key(token)));
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
