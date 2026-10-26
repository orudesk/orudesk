package com.orudesk.orudesk;

import com.orudesk.orudesk.identity.application.port.in.AssignRoleUseCase;
import com.orudesk.orudesk.identity.application.port.in.GetUserProfileQuery;
import com.orudesk.orudesk.identity.application.port.in.RegisterUserCommand;
import com.orudesk.orudesk.identity.application.port.in.RegisterUserUseCase;
import com.orudesk.orudesk.identity.application.service.UserProfileService;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.TenantId;
import com.orudesk.orudesk.identity.domain.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves three things about the user-profiles cache: it really lands in Redis, it really answers instead of
 * the database, and assigning a role really throws the old copy away.
 *
 * Not @Transactional: Redis is not part of the database transaction, so a rollback would not undo it anyway.
 * @AfterEach cleans both stores by hand.
 */
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ProfileCacheTest {

    private static final TenantId DEMO = new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    @Autowired
    private RegisterUserUseCase registerUser;

    @Autowired
    private AssignRoleUseCase assignRole;

    @Autowired
    private GetUserProfileQuery getUserProfile;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private StringRedisTemplate redis;

    private UserId userId;

    @BeforeEach
    void aRegisteredUser() {
        userId = registerUser.register(new RegisterUserCommand(DEMO, new Email("test-cache@orudesk.org"), "user-password"));
    }

    @AfterEach
    void cleanUp() {
        redis.delete(cacheKey());
        jdbc.update("DELETE FROM iam_user WHERE email LIKE 'test-%@orudesk.org'");
    }

    @Test
    void theFirstReadIsStoredInRedis() {
        getUserProfile.getProfile(userId);

        assertThat(redis.opsForValue().get(cacheKey())).contains("test-cache@orudesk.org");
        assertThat(redis.getExpire(cacheKey())).isBetween(1L, 600L);
    }

    @Test
    void laterReadsComeFromTheCacheNotTheDatabase() {
        getUserProfile.getProfile(userId);

        // Change the row behind the cache's back. A cached read can't see it: that is what "stale" means.
        jdbc.update("UPDATE iam_user SET enabled = false WHERE id = ?", userId.value());

        assertThat(getUserProfile.getProfile(userId).enabled()).isTrue();
    }

    @Test
    void assigningARoleEvictsTheCachedProfile() {
        assertThat(getUserProfile.getProfile(userId).roles()).containsExactly("USER");

        assignRole.assignRole(userId, RoleCode.ADMIN);

        assertThat(redis.hasKey(cacheKey())).isFalse();
        assertThat(getUserProfile.getProfile(userId).roles()).containsExactly("ADMIN", "USER");
    }

    private String cacheKey() {
        return "orudesk:cache:" + UserProfileService.CACHE + "::" + userId.value();
    }
}
