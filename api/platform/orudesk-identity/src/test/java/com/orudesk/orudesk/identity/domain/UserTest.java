package com.orudesk.orudesk.identity.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static final TenantId DEMO = new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    private User newUser() {
        return User.register(DEMO, new Email("karthikeyan@orudesk.com"), new PasswordHash("hashed"));
    }

    @Test
    void newUserIsEnabledAndHasTheUserRole() {
        User user = newUser();
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.roles()).containsExactly(RoleCode.USER);
    }

    @Test
    void assigningTheSameRoleTwiceKeepsOneCopy() {
        User user = newUser();
        user.assignRole(RoleCode.ADMIN);
        user.assignRole(RoleCode.ADMIN);
        assertThat(user.roles()).containsExactlyInAnyOrder(RoleCode.USER, RoleCode.ADMIN);
    }

    @Test
    void cannotRemoveTheLastRole() {
        User user = newUser();
        assertThatThrownBy(() -> user.removeRole(RoleCode.USER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least one role");
    }

    @Test
    void revokesOneGrantAndLeavesTheSameRoleElsewhere() {
        User user = newUser();
        OrgUnitId school = OrgUnitId.newId();
        user.assignRole(RoleCode.ADMIN);
        user.assignRoleAt(RoleCode.ADMIN, school);

        user.revoke(RoleGrant.at(RoleCode.ADMIN, school));

        assertThat(user.grants()).containsExactlyInAnyOrder(
                RoleGrant.tenantWide(RoleCode.USER), RoleGrant.tenantWide(RoleCode.ADMIN));
    }

    @Test
    void cannotAssignARoleToADisabledUser() {
        User user = newUser();
        user.disable();
        assertThatThrownBy(() -> user.assignRole(RoleCode.ADMIN))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rolesCannotBeChangedFromOutside() {
        User user = newUser();
        Set<RoleCode> roles = user.roles();
        assertThatThrownBy(() -> roles.add(RoleCode.ADMIN))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void aScopedRoleIsAGrantButNotATenantWideRole() {
        User user = User.register(DEMO, new Email("priya@orudesk.org"), new PasswordHash("hashed"));
        OrgUnitId schoolA = OrgUnitId.newId();

        user.assignRoleAt(RoleCode.ADMIN, schoolA);
        user.assignRoleAt(RoleCode.ADMIN, schoolA);   // twice is still one grant

        assertThat(user.grants()).containsExactlyInAnyOrder(
                RoleGrant.tenantWide(RoleCode.USER), RoleGrant.at(RoleCode.ADMIN, schoolA));
        assertThat(user.roles()).containsExactly(RoleCode.USER);   // only tenant-wide roles go in the token
        assertThat(user.hasRole(RoleCode.ADMIN)).isFalse();
    }
}
