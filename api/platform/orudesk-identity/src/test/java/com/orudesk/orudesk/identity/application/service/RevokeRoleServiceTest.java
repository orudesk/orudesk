package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.RoleRevoked;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.OrgUnitId;
import com.orudesk.orudesk.identity.domain.PasswordHash;
import com.orudesk.orudesk.identity.domain.RoleChangeNotAllowedException;
import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.RoleGrant;
import com.orudesk.orudesk.identity.domain.TenantId;
import com.orudesk.orudesk.identity.domain.User;
import com.orudesk.orudesk.identity.domain.UserId;
import com.orudesk.orudesk.identity.domain.UserNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RevokeRoleServiceTest {

    private static final TenantId DEMO = new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final OrgUnitId SCHOOL = OrgUnitId.newId();

    private final InMemoryUserStore store = new InMemoryUserStore();
    private final List<Object> events = new ArrayList<>();
    private final RevokeRoleService service =
            new RevokeRoleService(store, store, new InMemoryOrgUnitStore(), events::add);
    private final UserId admin = UserId.newId();   // whoever is doing it, unless a test says otherwise

    private User existingUser() {
        User user = User.register(DEMO, new Email("anna@orudesk.org"), new PasswordHash("hashed"));
        user.assignRole(RoleCode.ADMIN);
        user.assignRoleAt(RoleCode.ADMIN, SCHOOL);
        store.save(user);
        return user;
    }

    @Test
    void takesAwayOnlyTheScopedGrantWhenAUnitIsNamed() {
        User user = existingUser();

        service.revokeRole(user.id(), RoleCode.ADMIN, SCHOOL, admin);

        assertThat(store.findById(user.id()).orElseThrow().grants()).containsExactlyInAnyOrder(
                RoleGrant.tenantWide(RoleCode.USER), RoleGrant.tenantWide(RoleCode.ADMIN));
    }

    @Test
    void takesAwayTheTenantWideGrantWhenNoUnitIsNamed() {
        User user = existingUser();

        service.revokeRole(user.id(), RoleCode.ADMIN, null, admin);

        assertThat(store.findById(user.id()).orElseThrow().grants()).containsExactlyInAnyOrder(
                RoleGrant.tenantWide(RoleCode.USER), RoleGrant.at(RoleCode.ADMIN, SCHOOL));
    }

    @Test
    void anAdminCannotTakeAwayTheirOwnAdminRole() {
        User user = existingUser();

        assertThatThrownBy(() -> service.revokeRole(user.id(), RoleCode.ADMIN, null, user.id()))
                .isInstanceOf(RoleChangeNotAllowedException.class)
                .hasMessageContaining("your own ADMIN role");
        assertThat(store.findById(user.id()).orElseThrow().hasRole(RoleCode.ADMIN)).isTrue();
    }

    @Test
    void butMayGiveUpAScopedOne() {
        User user = existingUser();

        service.revokeRole(user.id(), RoleCode.ADMIN, SCHOOL, user.id());

        assertThat(store.findById(user.id()).orElseThrow().grants())
                .doesNotContain(RoleGrant.at(RoleCode.ADMIN, SCHOOL));
    }

    @Test
    void neverLeavesAUserWithoutARole() {
        User user = User.register(DEMO, new Email("tom@orudesk.org"), new PasswordHash("hashed"));
        store.save(user);

        assertThatThrownBy(() -> service.revokeRole(user.id(), RoleCode.USER, null, admin))
                .isInstanceOf(RoleChangeNotAllowedException.class)
                .hasMessageContaining("at least one role");
    }

    @Test
    void rejectsAUserThatDoesNotExist() {
        assertThatThrownBy(() -> service.revokeRole(UserId.newId(), RoleCode.ADMIN, null, admin))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void aRevokeIsRecordedOnlyWhenItTookSomethingAway() {
        User user = existingUser();

        service.revokeRole(user.id(), RoleCode.ADMIN, null, admin);
        service.revokeRole(user.id(), RoleCode.ADMIN, null, admin);   // again: nothing left to take

        assertThat(events).singleElement().isInstanceOfSatisfying(RoleRevoked.class, event ->
                assertThat(event.summary()).isEqualTo("Took ADMIN in the whole organisation from anna@orudesk.org"));
    }
}
