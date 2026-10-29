package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.ClientInfo;
import com.orudesk.orudesk.identity.application.port.in.OwnPasswordChanged;
import com.orudesk.orudesk.identity.application.port.out.RefreshGrant;
import com.orudesk.orudesk.identity.domain.BreachedPasswordException;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.PasswordHash;
import com.orudesk.orudesk.identity.domain.TenantId;
import com.orudesk.orudesk.identity.domain.User;
import com.orudesk.orudesk.identity.domain.WrongPasswordException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChangePasswordServiceTest {

    private static final TenantId DEMO = new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    private final InMemoryUserStore users = new InMemoryUserStore();
    private final InMemoryRefreshTokenStore sessions = new InMemoryRefreshTokenStore();
    private final List<Object> events = new ArrayList<>();
    private final ChangePasswordService service = new ChangePasswordService(users, users,
            (raw, hash) -> hash.value().equals("hashed:" + raw), raw -> new PasswordHash("hashed:" + raw),
            raw -> raw.equals("password123"), sessions, events::add);

    private User asha;

    @BeforeEach
    void user() {
        asha = User.register(DEMO, new Email("asha@orudesk.org"), new PasswordHash("hashed:old-password"));
        users.save(asha);
    }

    @Test
    void changesThePasswordAndSignsOutEveryOtherSession() {
        RefreshGrant here = sessions.start(asha.id(), ClientInfo.unknown());
        RefreshGrant phone = sessions.start(asha.id(), ClientInfo.unknown());
        RefreshGrant tablet = sessions.start(asha.id(), ClientInfo.unknown());

        service.changePassword(asha.id(), here.sessionId(), "old-password", "new-password");

        assertThat(users.findById(asha.id()).orElseThrow().passwordHash().value()).isEqualTo("hashed:new-password");
        assertThat(sessions.isActive(here.sessionId())).isTrue();
        assertThat(sessions.isActive(phone.sessionId())).isFalse();
        assertThat(sessions.isActive(tablet.sessionId())).isFalse();
        assertThat(events).singleElement().isInstanceOfSatisfying(OwnPasswordChanged.class,
                e -> assertThat(e.summary()).isEqualTo("asha@orudesk.org changed their password; signed out of 2 other sessions"));
    }

    @Test
    void theWrongCurrentPasswordOrABreachedNewOneChangesNothing() {
        assertThatThrownBy(() -> service.changePassword(asha.id(), null, "not-it", "new-password"))
                .isInstanceOf(WrongPasswordException.class);
        assertThatThrownBy(() -> service.changePassword(asha.id(), null, "old-password", "password123"))
                .isInstanceOf(BreachedPasswordException.class);

        assertThat(users.findById(asha.id()).orElseThrow().passwordHash().value()).isEqualTo("hashed:old-password");
        assertThat(events).isEmpty();
    }
}
