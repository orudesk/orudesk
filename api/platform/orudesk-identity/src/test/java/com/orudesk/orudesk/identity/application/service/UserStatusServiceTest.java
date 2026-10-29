package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.UserDisabled;
import com.orudesk.orudesk.identity.application.port.in.UserEnabled;
import com.orudesk.orudesk.identity.application.port.out.BlockedUsers;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.PasswordHash;
import com.orudesk.orudesk.identity.domain.SelfLockoutException;
import com.orudesk.orudesk.identity.domain.TenantId;
import com.orudesk.orudesk.identity.domain.User;
import com.orudesk.orudesk.identity.domain.UserId;
import com.orudesk.orudesk.identity.domain.UserNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserStatusServiceTest {

    private static final TenantId DEMO = new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    private final InMemoryUserStore store = new InMemoryUserStore();
    private final Set<UserId> blocked = new HashSet<>();
    private final BlockedUsers blockedUsers = new BlockedUsers() {
        @Override public void block(UserId userId) { blocked.add(userId); }
        @Override public void unblock(UserId userId) { blocked.remove(userId); }
        @Override public boolean isBlocked(UserId userId) { return blocked.contains(userId); }
    };
    private final List<Object> events = new ArrayList<>();
    private final UserStatusService service = new UserStatusService(store, store, blockedUsers, events::add);
    private final UserId admin = UserId.newId();

    private User existingUser() {
        User user = User.register(DEMO, new Email("ravi@orudesk.org"), new PasswordHash("hashed"));
        store.save(user);
        return user;
    }

    @Test
    void disablingStopsLoginBlocksTheirTokensAndIsAudited() {
        User user = existingUser();

        service.disable(user.id(), admin);

        assertThat(store.findById(user.id()).orElseThrow().isEnabled()).isFalse();
        assertThat(blocked).containsExactly(user.id());
        assertThat(events).containsExactly(new UserDisabled(DEMO.value(), user.id().value(), "ravi@orudesk.org"));
    }

    @Test
    void enablingUndoesBothAndKeepsTheirRoles() {
        User user = existingUser();
        service.disable(user.id(), admin);
        events.clear();

        service.enable(user.id());

        User again = store.findById(user.id()).orElseThrow();
        assertThat(again.isEnabled()).isTrue();
        assertThat(again.grants()).isEqualTo(user.grants());
        assertThat(blocked).isEmpty();
        assertThat(events).containsExactly(new UserEnabled(DEMO.value(), user.id().value(), "ravi@orudesk.org"));
    }

    @Test
    void doingItTwiceIsFineAndRecordedOnce() {
        User user = existingUser();

        service.disable(user.id(), admin);
        service.disable(user.id(), admin);
        service.enable(user.id());
        service.enable(user.id());

        assertThat(events).hasSize(2);
    }

    @Test
    void anAdminCannotDisableThemselves() {
        User user = existingUser();

        assertThatThrownBy(() -> service.disable(user.id(), user.id()))
                .isInstanceOf(SelfLockoutException.class);
        assertThat(store.findById(user.id()).orElseThrow().isEnabled()).isTrue();
    }

    @Test
    void anUnknownUserIsNotFound() {
        assertThatThrownBy(() -> service.disable(UserId.newId(), admin)).isInstanceOf(UserNotFoundException.class);
        assertThatThrownBy(() -> service.enable(UserId.newId())).isInstanceOf(UserNotFoundException.class);
    }
}
