package com.orudesk.orudesk.identity.adapter.out.redis;

import com.orudesk.orudesk.identity.application.port.out.BlockedUsers;
import com.orudesk.orudesk.identity.domain.UserId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Key "orudesk:blocked-user:{user id}", expiring after the access token lifetime: by then every token the user
 * had has expired by itself. Redis, not the database, because it's asked on every authenticated request.
 */
@Component
class RedisBlockedUsers implements BlockedUsers {

    private static final String KEY_PREFIX = "orudesk:blocked-user:";

    private final StringRedisTemplate redis;
    private final Duration ttl;

    RedisBlockedUsers(StringRedisTemplate redis, @Value("${orudesk.security.jwt.access-token-ttl}") Duration ttl) {
        this.redis = redis;
        this.ttl = ttl;
    }

    @Override
    public void block(UserId userId) {
        redis.opsForValue().set(key(userId), "1", ttl);
    }

    @Override
    public void unblock(UserId userId) {
        redis.delete(key(userId));
    }

    @Override
    public boolean isBlocked(UserId userId) {
        return Boolean.TRUE.equals(redis.hasKey(key(userId)));
    }

    private static String key(UserId userId) {
        return KEY_PREFIX + userId.value();
    }
}
