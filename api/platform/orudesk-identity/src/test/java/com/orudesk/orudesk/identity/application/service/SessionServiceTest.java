package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.ActiveSession;
import com.orudesk.orudesk.identity.application.port.in.ClientInfo;
import com.orudesk.orudesk.identity.application.port.in.SessionsEnded;
import com.orudesk.orudesk.identity.application.port.out.RefreshGrant;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.PasswordHash;
import com.orudesk.orudesk.identity.domain.SessionNotFoundException;
import com.orudesk.orudesk.identity.domain.TenantId;
import com.orudesk.orudesk.identity.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionServiceTest {

    private static final TenantId DEMO = new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    private final InMemoryUserStore users = new InMemoryUserStore();
    private final InMemoryRefreshTokenStore store = new InMemoryRefreshTokenStore();
    private final List<Object> published = new ArrayList<>();
    private final SessionService service = new SessionService(store, users, published::add);

    private User asha;
    private User ravi;

    @BeforeEach
    void users() {
        asha = User.register(DEMO, new Email("asha@orudesk.org"), new PasswordHash("x"));
        ravi = User.register(DEMO, new Email("ravi@orudesk.org"), new PasswordHash("x"));
        users.save(asha);
        users.save(ravi);
    }

    @Test
    void listsOnlyTheUsersOwnSessionsAndMarksTheCurrentOne() {
        RefreshGrant laptop = store.start(asha.id(), new ClientInfo("Firefox on Windows", "203.0.113.7"));
        store.start(asha.id(), new ClientInfo("Chrome on Android", "198.51.100.2"));
        store.start(ravi.id(), ClientInfo.unknown());

        List<ActiveSession> sessions = service.list(asha.id(), laptop.sessionId());

        assertThat(sessions).hasSize(2);
        assertThat(sessions).filteredOn(ActiveSession::current).singleElement()
                .satisfies(s -> assertThat(s.device()).isEqualTo("Firefox on Windows"));
    }

    @Test
    void endingAnotherUsersSessionLooksLikeItDoesNotExist() {
        RefreshGrant ravis = store.start(ravi.id(), ClientInfo.unknown());

        assertThatThrownBy(() -> service.end(asha.id(), ravis.sessionId())).isInstanceOf(SessionNotFoundException.class);
        assertThat(store.isActive(ravis.sessionId())).isTrue();
        assertThat(published).isEmpty();
    }

    @Test
    void signingOutEverywhereElseKeepsTheCurrentSessionAndIsAuditedOnce() {
        RefreshGrant here = store.start(asha.id(), ClientInfo.unknown());
        RefreshGrant phone = store.start(asha.id(), ClientInfo.unknown());
        RefreshGrant tablet = store.start(asha.id(), ClientInfo.unknown());

        assertThat(service.endOthers(asha.id(), here.sessionId())).isEqualTo(2);

        assertThat(store.isActive(here.sessionId())).isTrue();
        assertThat(store.isActive(phone.sessionId())).isFalse();
        assertThat(store.isActive(tablet.sessionId())).isFalse();
        assertThat(published).singleElement().isInstanceOfSatisfying(SessionsEnded.class,
                e -> assertThat(e.summary()).isEqualTo("Signed asha@orudesk.org out of 2 sessions"));
    }

    @Test
    void nothingToEndIsNotAudited() {
        RefreshGrant here = store.start(asha.id(), ClientInfo.unknown());

        assertThat(service.endOthers(asha.id(), here.sessionId())).isZero();
        assertThat(published).isEmpty();
    }
}
