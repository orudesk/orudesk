package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.OrgUnit;
import com.orudesk.orudesk.identity.domain.OrgUnitId;
import com.orudesk.orudesk.identity.domain.OrgUnitNotFoundException;
import com.orudesk.orudesk.identity.domain.OrgUnitType;
import com.orudesk.orudesk.identity.domain.PasswordHash;
import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.RoleGrant;
import com.orudesk.orudesk.identity.domain.TenantId;
import com.orudesk.orudesk.identity.domain.UnknownRoleException;
import com.orudesk.orudesk.identity.domain.User;
import com.orudesk.orudesk.identity.domain.UserId;
import com.orudesk.orudesk.identity.domain.UserNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssignRoleServiceTest {

    private static final TenantId DEMO = new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    private final InMemoryUserStore store = new InMemoryUserStore();
    private final InMemoryOrgUnitStore units = new InMemoryOrgUnitStore();
    private final AssignRoleService service = new AssignRoleService(store, store, store, units);

    private UserId existingUser() {
        User user = User.register(DEMO, new Email("karthikeyan@orudesk.com"), new PasswordHash("hashed"));
        store.save(user);
        return user.id();
    }

    @Test
    void assignsAKnownRole() {
        UserId id = existingUser();

        service.assignRole(id, RoleCode.ADMIN);

        assertThat(store.findById(id).orElseThrow().hasRole(RoleCode.ADMIN)).isTrue();
    }

    @Test
    void rejectsARoleThatDoesNotExist() {
        UserId id = existingUser();

        assertThatThrownBy(() -> service.assignRole(id, new RoleCode("PILOT")))
                .isInstanceOf(UnknownRoleException.class);
    }

    @Test
    void rejectsAUserThatDoesNotExist() {
        assertThatThrownBy(() -> service.assignRole(UserId.newId(), RoleCode.ADMIN))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void grantsARoleInOneUnit() {
        OrgUnit school = OrgUnit.restore(OrgUnitId.newId(), null, new OrgUnitType("SCHOOL"), "sunrise-a", "Sunrise School A");
        units.save(school);
        UserId id = existingUser();

        service.assignRoleAt(id, RoleCode.ADMIN, school.id());

        User user = store.findById(id).orElseThrow();
        assertThat(user.grants()).contains(RoleGrant.at(RoleCode.ADMIN, school.id()));
        assertThat(user.hasRole(RoleCode.ADMIN)).isFalse();   // not tenant-wide
    }

    @Test
    void refusesAUnitTheTenantDoesNotHave() {
        UserId id = existingUser();

        assertThatThrownBy(() -> service.assignRoleAt(id, RoleCode.ADMIN, OrgUnitId.newId()))
                .isInstanceOf(OrgUnitNotFoundException.class);
    }
}
