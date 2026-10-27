package com.orudesk.orudesk.identity.adapter.out.redis;

import com.orudesk.orudesk.identity.application.port.out.LoginAttemptLimiter;
import com.orudesk.orudesk.identity.domain.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Failed logins per email, in Redis: key "orudesk:login-failures:{sha256 of email}", value = how many, expiring
 * `window` after the first failure. At `max-failures` the email is blocked until the key expires.
 *
 * In Redis rather than in memory, so the count is shared by every API instance and survives a restart; and a
 * hash of the email, so Redis holds no addresses.
 */
@Component
class RedisLoginAttemptLimiter implements LoginAttemptLimiter {

    private static final String KEY_PREFIX = "orudesk:login-failures:";

    /**
     * INCR and, on the first failure, PEXPIRE, as one atomic step. Two separate calls could leave a counter
     * without an expiry (the app stops in between): a lockout that never ends.
     */
    private static final RedisScript<Long> COUNT_FAILURE = RedisScript.of("""
            local failures = redis.call('INCR', KEYS[1])
            if failures == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end
            return failures
            """, Long.class);

    private final StringRedisTemplate redis;
    private final int maxFailures;
    private final Duration window;

    RedisLoginAttemptLimiter(StringRedisTemplate redis,
                             @Value("${orudesk.security.login.max-failures:5}") int maxFailures,
                             @Value("${orudesk.security.login.window:15m}") Duration window) {
        this.redis = redis;
        this.maxFailures = maxFailures;
        this.window = window;
    }

    @Override
    public Optional<Duration> blockedFor(Email email) {
        String failures = redis.opsForValue().get(key(email));
        if (failures == null || Long.parseLong(failures) < maxFailures) {
            return Optional.empty();
        }
        Long millis = redis.getExpire(key(email), TimeUnit.MILLISECONDS);
        return Optional.of(millis == null || millis < 0 ? window : Duration.ofMillis(millis));
    }

    @Override
    public void failed(Email email) {
        redis.execute(COUNT_FAILURE, List.of(key(email)), Long.toString(window.toMillis()));
    }

    @Override
    public void succeeded(Email email) {
        redis.delete(key(email));
    }

    private static String key(Email email) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(email.value().getBytes(StandardCharsets.UTF_8));
            return KEY_PREFIX + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available in Java", e);
        }
    }
}
