package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.PasswordChanged;
import com.orudesk.orudesk.identity.application.port.in.PasswordResetRequested;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.InvalidPasswordResetTokenException;
import com.orudesk.orudesk.identity.domain.PasswordHash;
import com.orudesk.orudesk.identity.domain.TenantId;
import com.orudesk.orudesk.identity.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordResetServiceTest {

    private static final TenantId DEMO = new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final Email EMAIL = new Email("asha@orudesk.org");

    private final InMemoryUserStore users = new InMemoryUserStore();
    private final InMemoryPasswordResetTokens resetTokens = new InMemoryPasswordResetTokens();
    private final InMemoryRefreshTokenStore refreshTokens = new InMemoryRefreshTokenStore();
    private final InMemoryLoginAttemptLimiter loginAttempts = new InMemoryLoginAttemptLimiter();
    private final List<Object> events = new ArrayList<>();
    private final PasswordResetService service = new PasswordResetService(users, users, raw -> new PasswordHash("hashed:" + raw),
            resetTokens, refreshTokens, loginAttempts, events::add, Clock.fixed(Instant.parse("2026-10-29T10:00:00Z"), ZoneOffset.UTC));

    private User user;

    @BeforeEach
    void createUser() {
        user = User.register(DEMO, EMAIL, new PasswordHash("hashed:old-password"));
        users.save(user);
    }

    @Test
    void aRequestForAKnownEmailIsPublishedWithoutAnySecret() {
        service.requestReset(EMAIL);

        assertThat(events).containsExactly(new PasswordResetRequested(user.id().value(), "asha@orudesk.org"));
    }

    @Test
    void anUnknownEmailOrADisabledUserGetsNothingAndNoError() {
        service.requestReset(new Email("nobody@orudesk.org"));
        user.disable();
        users.save(user);
        service.requestReset(EMAIL);

        assertThat(events).isEmpty();
        assertThat(service.issueToken(user.id())).isEmpty();
    }

    @Test
    void theTokenSetsTheNewPasswordEndsEverySessionAndLiftsTheLoginBlock() {
        refreshTokens.create(user.id());
        refreshTokens.create(user.id());
        for (int i = 0; i < InMemoryLoginAttemptLimiter.MAX_FAILURES; i++) {
            loginAttempts.failed(EMAIL);
        }
        String token = service.issueToken(user.id()).orElseThrow();

        service.resetPassword(token, "new-password");

        assertThat(users.findById(user.id()).orElseThrow().passwordHash()).isEqualTo(new PasswordHash("hashed:new-password"));
        assertThat(refreshTokens.size()).isZero();
        assertThat(loginAttempts.failures(EMAIL)).isZero();
        assertThat(events).containsExactly(new PasswordChanged(DEMO.value(), user.id().value(), "asha@orudesk.org"));
    }

    @Test
    void aTokenWorksOnceAndOnlyTheNewestOneWorks() {
        String first = service.issueToken(user.id()).orElseThrow();
        String second = service.issueToken(user.id()).orElseThrow();

        assertThatThrownBy(() -> service.resetPassword(first, "new-password"))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
        service.resetPassword(second, "new-password");
        assertThatThrownBy(() -> service.resetPassword(second, "another-password"))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
    }

    @Test
    void aUserDisabledAfterTheEmailCannotUseTheLink() {
        String token = service.issueToken(user.id()).orElseThrow();
        user.disable();
        users.save(user);

        assertThatThrownBy(() -> service.resetPassword(token, "new-password"))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
        assertThat(users.findById(user.id()).orElseThrow().passwordHash()).isEqualTo(new PasswordHash("hashed:old-password"));
    }
}
