package com.orudesk.orudesk.identity.adapter.out.redis;

import com.orudesk.orudesk.identity.application.port.out.RefreshTokenStore;
import com.orudesk.orudesk.identity.domain.UserId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps refresh tokens in Redis: key "orudesk:refresh:{sha256 of token}", value = user id,
 * expiring by itself after the refresh token lifetime.
 * Only a hash of the token is stored, so someone who reads Redis can't use the tokens.
 *
 * Day 53: each user also has a set "orudesk:refresh-by-user:{user id}" of their token keys, so all their sessions
 * can end at once (after a password reset). The set expires with their newest token, so it never outlives them.
 */
@Component
class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "orudesk:refresh:";
    private static final String BY_USER_PREFIX = "orudesk:refresh-by-user:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;
    private final Duration ttl;

    RedisRefreshTokenStore(StringRedisTemplate redis,
                           @Value("${orudesk.security.jwt.refresh-token-ttl}") Duration ttl) {
        this.redis = redis;
        this.ttl = ttl;
    }

    @Override
    public String create(UserId userId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String key = key(token);
        redis.opsForValue().set(key, userId.value().toString(), ttl);
        String byUser = BY_USER_PREFIX + userId.value();
        redis.opsForSet().add(byUser, key);
        redis.expire(byUser, ttl);
        return token;
    }

    @Override
    public Optional<UserId> consume(String refreshToken) {
        // GETDEL reads and deletes in one atomic step, so two requests can't both use the same token.
        String userId = redis.opsForValue().getAndDelete(key(refreshToken));
        return Optional.ofNullable(userId).map(id -> new UserId(UUID.fromString(id)));
    }

    @Override
    public void revoke(String refreshToken) {
        redis.delete(key(refreshToken));
    }

    @Override
    public void revokeAll(UserId userId) {
        String byUser = BY_USER_PREFIX + userId.value();
        Set<String> keys = redis.opsForSet().members(byUser);
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);   // keys already used or expired are simply not there: deleting them is harmless
        }
        redis.delete(byUser);
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
