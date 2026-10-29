package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.RegisterUserCommand;
import com.orudesk.orudesk.identity.domain.BreachedPasswordException;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.EmailAlreadyRegisteredException;
import com.orudesk.orudesk.identity.domain.PasswordHash;
import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.TenantId;
import com.orudesk.orudesk.identity.domain.User;
import com.orudesk.orudesk.identity.domain.UserId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegisterUserServiceTest {

    private static final TenantId DEMO = new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    private final InMemoryUserStore store = new InMemoryUserStore();
    private final RegisterUserService service =
            new RegisterUserService(store, store, raw -> new PasswordHash("hashed:" + raw),
                    raw -> raw.equals("password123"), event -> { });

    @Test
    void registersAUserWithAHashedPasswordAndTheUserRole() {
        UserId id = service.register(new RegisterUserCommand(DEMO, new Email("karthikeyan@orudesk.com"), "secret-password"));

        User saved = store.findById(id).orElseThrow();
        assertThat(saved.email()).isEqualTo(new Email("karthikeyan@orudesk.com"));
        assertThat(saved.passwordHash().value()).isEqualTo("hashed:secret-password");
        assertThat(saved.roles()).containsExactly(RoleCode.USER);
    }

    @Test
    void rejectsAnEmailThatIsAlreadyRegistered() {
        service.register(new RegisterUserCommand(DEMO, new Email("karthikeyan@orudesk.com"), "secret-password"));

        assertThatThrownBy(() -> service.register(
                new RegisterUserCommand(DEMO, new Email("KARTHIKEYAN@orudesk.com"), "another-password")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void rejectsAPasswordKnownFromBreaches() {
        assertThatThrownBy(() -> service.register(
                new RegisterUserCommand(DEMO, new Email("karthikeyan@orudesk.com"), "password123")))
                .isInstanceOf(BreachedPasswordException.class)
                .satisfies(e -> assertThat(((BreachedPasswordException) e).field()).isEqualTo("password"));
        assertThat(store.existsByEmail(new Email("karthikeyan@orudesk.com"))).isFalse();
    }

    @Test
    void aPasswordFromTheConfigurationIsNotChecked() {
        // The first admin's or the demo's: refusing it would stop the application from starting
        service.register(RegisterUserCommand.fromConfiguration(DEMO, new Email("admin@orudesk.com"), "password123"));

        assertThat(store.existsByEmail(new Email("admin@orudesk.com"))).isTrue();
    }

    @Test
    void rejectsAShortPassword() {
        assertThatThrownBy(() -> new RegisterUserCommand(DEMO, new Email("karthikeyan@orudesk.com"), "short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8 to 72");
    }
}
