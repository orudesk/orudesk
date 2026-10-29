package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.TotpSetup;
import com.orudesk.orudesk.identity.application.port.in.TwoFactorDisabled;
import com.orudesk.orudesk.identity.application.port.in.TwoFactorEnabled;
import com.orudesk.orudesk.identity.domain.Base32;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.PasswordHash;
import com.orudesk.orudesk.identity.domain.TenantId;
import com.orudesk.orudesk.identity.domain.Totp;
import com.orudesk.orudesk.identity.domain.TwoFactorAlreadyEnabledException;
import com.orudesk.orudesk.identity.domain.User;
import com.orudesk.orudesk.identity.domain.WrongPasswordException;
import com.orudesk.orudesk.identity.domain.WrongTwoFactorCodeException;
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

class TwoFactorServiceTest {

    private static final TenantId DEMO = new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final Instant NOW = Instant.parse("2026-10-29T10:00:00Z");

    private final InMemoryUserStore users = new InMemoryUserStore();
    private final InMemoryTotpStore store = new InMemoryTotpStore();
    private final List<Object> published = new ArrayList<>();
    private final TwoFactorService service = new TwoFactorService(users, (raw, hash) -> hash.value().equals("hashed:" + raw),
            store, new PlainSecretBox(), published::add, Clock.fixed(NOW, ZoneOffset.UTC));

    private User asha;

    @BeforeEach
    void user() {
        asha = User.register(DEMO, new Email("asha@orudesk.org"), new PasswordHash("hashed:asha-password"));
        users.save(asha);
    }

    @Test
    void setUpWithTheAppsFirstCodeThenTurnOff() {
        TotpSetup setup = service.beginSetup(asha.id(), "asha-password");

        assertThat(setup.otpauthUri()).startsWith("otpauth://totp/Orudesk%3Aasha%40orudesk.org?secret=" + setup.secret())
                .contains("&issuer=Orudesk&algorithm=SHA1&digits=6&period=30");
        assertThat(service.status(asha.id()).enabled()).isFalse();   // not until a code proves the app works

        List<String> codes = service.confirmSetup(asha.id(), codeNow(setup));

        assertThat(codes).hasSize(10);
        assertThat(service.status(asha.id())).satisfies(s -> {
            assertThat(s.enabled()).isTrue();
            assertThat(s.enabledAt()).isEqualTo(NOW);
            assertThat(s.recoveryCodesLeft()).isEqualTo(10);
        });

        service.disable(asha.id(), "asha-password");

        assertThat(service.status(asha.id()).enabled()).isFalse();
        assertThat(published).hasExactlyElementsOfTypes(TwoFactorEnabled.class, TwoFactorDisabled.class);
    }

    @Test
    void aWrongCodeLeavesItOff() {
        service.beginSetup(asha.id(), "asha-password");

        assertThatThrownBy(() -> service.confirmSetup(asha.id(), "000000")).isInstanceOf(WrongTwoFactorCodeException.class);
        assertThat(service.status(asha.id()).enabled()).isFalse();
        assertThat(published).isEmpty();
    }

    @Test
    void everyChangeAsksForTheCurrentPassword() {
        assertThatThrownBy(() -> service.beginSetup(asha.id(), "wrong")).isInstanceOf(WrongPasswordException.class);
        TotpSetup setup = service.beginSetup(asha.id(), "asha-password");
        service.confirmSetup(asha.id(), codeNow(setup));

        assertThatThrownBy(() -> service.disable(asha.id(), "wrong")).isInstanceOf(WrongPasswordException.class);
        assertThatThrownBy(() -> service.newRecoveryCodes(asha.id(), "wrong")).isInstanceOf(WrongPasswordException.class);
        assertThat(service.status(asha.id()).enabled()).isTrue();
    }

    @Test
    void settingUpAgainWhileOnIsRefused() {
        TotpSetup setup = service.beginSetup(asha.id(), "asha-password");
        service.confirmSetup(asha.id(), codeNow(setup));

        assertThatThrownBy(() -> service.beginSetup(asha.id(), "asha-password"))
                .isInstanceOf(TwoFactorAlreadyEnabledException.class);
    }

    private static String codeNow(TotpSetup setup) {
        return Totp.code(Base32.decode(setup.secret()), Totp.step(NOW));
    }
}
