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
import java.util.UUID;

/**
 * Keeps refresh tokens in Redis: key "orudesk:refresh:{sha256 of token}", value = user id,
 * expiring by itself after the refresh token lifetime.
 * Only a hash of the token is stored, so someone who reads Redis can't use the tokens.
 */
@Component
class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "orudesk:refresh:";
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
        redis.opsForValue().set(key(token), userId.value().toString(), ttl);
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

    private static String key(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return KEY_PREFIX + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available in Java", e);
        }
    }
}
