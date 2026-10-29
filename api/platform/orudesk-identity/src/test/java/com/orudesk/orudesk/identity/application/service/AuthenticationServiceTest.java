package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.AuthTokens;
import com.orudesk.orudesk.identity.application.port.in.LoginCommand;
import com.orudesk.orudesk.identity.application.port.out.AccessToken;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.InvalidCredentialsException;
import com.orudesk.orudesk.identity.domain.InvalidRefreshTokenException;
import com.orudesk.orudesk.identity.domain.PasswordHash;
import com.orudesk.orudesk.identity.domain.TenantId;
import com.orudesk.orudesk.identity.domain.TooManyLoginAttemptsException;
import com.orudesk.orudesk.identity.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationServiceTest {

    private static final TenantId DEMO = new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final Email EMAIL = new Email("ck@orudesk.org");

    private final InMemoryUserStore users = new InMemoryUserStore();
    private final InMemoryRefreshTokenStore refreshTokens = new InMemoryRefreshTokenStore();
    private final InMemoryLoginAttemptLimiter loginAttempts = new InMemoryLoginAttemptLimiter();
    private final AuthenticationService service = new AuthenticationService(
            users,
            (raw, hash) -> hash.value().equals("hashed:" + raw),
            (user, session) -> new AccessToken("access-for-" + user.email().value(), 900),
            refreshTokens,
            loginAttempts,
            new SecondFactor(new InMemoryTotpStore(), new PlainSecretBox(), event -> { }, Clock.systemUTC()),
            new InMemoryPendingLogins());

    private User user;

    @BeforeEach
    void createUser() {
        user = User.register(DEMO, EMAIL, new PasswordHash("hashed:secret-password"));
        users.save(user);
    }

    @Test
    void loginWithTheRightPasswordReturnsTokens() {
        AuthTokens tokens = service.login(new LoginCommand(EMAIL, "secret-password"));

        assertThat(tokens.accessToken()).isEqualTo("access-for-ck@orudesk.org");
        assertThat(tokens.expiresInSeconds()).isEqualTo(900);
        assertThat(tokens.refreshToken()).isNotBlank();
    }

    @Test
    void aWrongPasswordAndAnUnknownEmailFailTheSameWay() {
        assertThatThrownBy(() -> service.login(new LoginCommand(EMAIL, "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("invalid email or password");
        assertThatThrownBy(() -> service.login(new LoginCommand(new Email("nobody@orudesk.org"), "secret-password")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("invalid email or password");
    }

    @Test
    void afterTooManyFailuresEvenTheRightPasswordIsRefusedForAWhile() {
        for (int i = 0; i < InMemoryLoginAttemptLimiter.MAX_FAILURES; i++) {
            assertThatThrownBy(() -> service.login(new LoginCommand(EMAIL, "wrong-password")))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        assertThatThrownBy(() -> service.login(new LoginCommand(EMAIL, "secret-password")))
                .isInstanceOf(TooManyLoginAttemptsException.class)
                .satisfies(e -> assertThat(((TooManyLoginAttemptsException) e).retryAfter()).isEqualTo(Duration.ofMinutes(15)));
        assertThat(refreshTokens.size()).isZero();   // no tokens were issued
    }

    @Test
    void unknownEmailsAreCountedToo() {
        Email nobody = new Email("nobody@orudesk.org");
        assertThatThrownBy(() -> service.login(new LoginCommand(nobody, "guess")))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(loginAttempts.failures(nobody)).isEqualTo(1);
    }

    @Test
    void aSuccessfulLoginStartsTheCountAgain() {
        assertThatThrownBy(() -> service.login(new LoginCommand(EMAIL, "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class);

        service.login(new LoginCommand(EMAIL, "secret-password"));

        assertThat(loginAttempts.failures(EMAIL)).isZero();
    }

    @Test
    void aDisabledUserCannotLogIn() {
        user.disable();

        assertThatThrownBy(() -> service.login(new LoginCommand(EMAIL, "secret-password")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void aRefreshTokenWorksOnlyOnce() {
        AuthTokens first = service.login(new LoginCommand(EMAIL, "secret-password"));

        AuthTokens second = service.refresh(first.refreshToken());

        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThatThrownBy(() -> service.refresh(first.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void logoutRevokesTheRefreshToken() {
        AuthTokens tokens = service.login(new LoginCommand(EMAIL, "secret-password"));

        service.logout(tokens.refreshToken());

        assertThatThrownBy(() -> service.refresh(tokens.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }
}
