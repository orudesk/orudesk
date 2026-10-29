package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.AuthTokens;
import com.orudesk.orudesk.identity.application.port.in.ClientInfo;
import com.orudesk.orudesk.identity.application.port.in.LoginCommand;
import com.orudesk.orudesk.identity.application.port.in.RecoveryCodeUsed;
import com.orudesk.orudesk.identity.application.port.in.SecondFactorCommand;
import com.orudesk.orudesk.identity.application.port.in.TotpSetup;
import com.orudesk.orudesk.identity.application.port.out.AccessToken;
import com.orudesk.orudesk.identity.domain.Base32;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.LoginExpiredException;
import com.orudesk.orudesk.identity.domain.PasswordHash;
import com.orudesk.orudesk.identity.domain.SecondFactorRequiredException;
import com.orudesk.orudesk.identity.domain.TenantId;
import com.orudesk.orudesk.identity.domain.TooManyLoginAttemptsException;
import com.orudesk.orudesk.identity.domain.Totp;
import com.orudesk.orudesk.identity.domain.User;
import com.orudesk.orudesk.identity.domain.WrongTwoFactorCodeException;
import com.orudesk.orudesk.identity.application.port.out.PasswordVerifier;
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

/** Day 68: the login's second step, for a user with two-factor sign-in on. */
class SecondFactorLoginTest {

    private static final TenantId DEMO = new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final Email EMAIL = new Email("asha@orudesk.org");
    private static final Instant NOW = Instant.parse("2026-10-29T10:00:00Z");

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final PasswordVerifier passwords = (raw, hash) -> hash.value().equals("hashed:" + raw);
    private final InMemoryUserStore users = new InMemoryUserStore();
    private final InMemoryTotpStore totps = new InMemoryTotpStore();
    private final InMemoryRefreshTokenStore refreshTokens = new InMemoryRefreshTokenStore();
    private final InMemoryLoginAttemptLimiter loginAttempts = new InMemoryLoginAttemptLimiter();
    private final InMemoryPendingLogins pending = new InMemoryPendingLogins();
    private final List<Object> events = new ArrayList<>();
    private final AuthenticationService service = new AuthenticationService(users, passwords,
            (user, session) -> new AccessToken("access-for-" + user.email().value(), 900), refreshTokens, loginAttempts,
            new SecondFactor(totps, new PlainSecretBox(), events::add, clock), pending);

    private byte[] secret;
    private List<String> recoveryCodes;

    @BeforeEach
    void userWithTwoFactorOn() {
        User user = User.register(DEMO, EMAIL, new PasswordHash("hashed:asha-password"));
        users.save(user);
        TwoFactorService twoFactor = new TwoFactorService(users, passwords, totps, new PlainSecretBox(), e -> { }, clock);
        TotpSetup setup = twoFactor.beginSetup(user.id(), "asha-password");
        secret = Base32.decode(setup.secret());
        // Set up a minute ago, so the code for now is unused
        recoveryCodes = new TwoFactorService(users, passwords, totps, new PlainSecretBox(), e -> { },
                Clock.fixed(NOW.minusSeconds(60), ZoneOffset.UTC))
                .confirmSetup(user.id(), Totp.code(secret, Totp.step(NOW.minusSeconds(60))));
    }

    @Test
    void theRightPasswordAsksForTheCodeAndTheCodeFinishesTheLogin() {
        String token = firstStep();
        assertThat(refreshTokens.size()).isZero();   // no session yet

        AuthTokens tokens = service.completeLogin(new SecondFactorCommand(token, codeNow(), ClientInfo.unknown()));

        assertThat(tokens.accessToken()).isEqualTo("access-for-asha@orudesk.org");
        assertThatThrownBy(() -> service.completeLogin(new SecondFactorCommand(token, codeNow(), ClientInfo.unknown())))
                .isInstanceOf(LoginExpiredException.class);   // the token was spent
    }

    @Test
    void aCodeWorksOnlyOnce() {
        service.completeLogin(new SecondFactorCommand(firstStep(), codeNow(), ClientInfo.unknown()));

        String again = firstStep();
        assertThatThrownBy(() -> service.completeLogin(new SecondFactorCommand(again, codeNow(), ClientInfo.unknown())))
                .isInstanceOf(WrongTwoFactorCodeException.class);
    }

    @Test
    void aRecoveryCodeWorksOnceAndIsAudited() {
        String code = recoveryCodes.getFirst();

        service.completeLogin(new SecondFactorCommand(firstStep(), code.toLowerCase(), ClientInfo.unknown()));

        assertThat(events).singleElement().isInstanceOfSatisfying(RecoveryCodeUsed.class,
                e -> assertThat(e.codesLeft()).isEqualTo(9));
        String again = firstStep();
        assertThatThrownBy(() -> service.completeLogin(new SecondFactorCommand(again, code, ClientInfo.unknown())))
                .isInstanceOf(WrongTwoFactorCodeException.class);
    }

    @Test
    void wrongCodesCountAgainstTheAccountLikeWrongPasswords() {
        String token = firstStep();
        for (int i = 0; i < InMemoryLoginAttemptLimiter.MAX_FAILURES; i++) {
            assertThatThrownBy(() -> service.completeLogin(new SecondFactorCommand(token, "000000", ClientInfo.unknown())))
                    .isInstanceOf(WrongTwoFactorCodeException.class);
        }

        // Even the right code, and a fresh start with the password, wait now
        assertThatThrownBy(() -> service.completeLogin(new SecondFactorCommand(token, codeNow(), ClientInfo.unknown())))
                .isInstanceOf(TooManyLoginAttemptsException.class);
        assertThatThrownBy(() -> service.login(new LoginCommand(EMAIL, "asha-password")))
                .isInstanceOf(TooManyLoginAttemptsException.class);
    }

    @Test
    void anUnknownTokenIsExpired() {
        assertThatThrownBy(() -> service.completeLogin(new SecondFactorCommand("made-up", codeNow(), ClientInfo.unknown())))
                .isInstanceOf(LoginExpiredException.class);
    }

    private String firstStep() {
        try {
            service.login(new LoginCommand(EMAIL, "asha-password"));
        } catch (SecondFactorRequiredException e) {
            return e.loginToken();
        }
        throw new AssertionError("the login should have asked for a second factor");
    }

    private String codeNow() {
        return Totp.code(secret, Totp.step(NOW));
    }
}
