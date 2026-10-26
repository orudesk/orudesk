package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.OrgUnit;
import com.orudesk.orudesk.identity.domain.OrgUnitId;
import com.orudesk.orudesk.identity.domain.OrgUnitNotFoundException;
import com.orudesk.orudesk.identity.domain.OrgUnitType;
import com.orudesk.orudesk.identity.domain.PasswordHash;
import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.TenantId;
import com.orudesk.orudesk.identity.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrgAccessServiceTest {

    private static final TenantId DEMO = new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final RoleCode PRINCIPAL = new RoleCode("PRINCIPAL");

    //  Sunrise Trust ─┬─ Sunrise Group ─┬─ School A
    //                 │                 └─ School B
    //                 └─ Sunrise Clinic
    private final OrgUnit trust = OrgUnit.restore(OrgUnitId.newId(), null, new OrgUnitType("ORGANISATION"), "sunrise", "Sunrise Trust");
    private final OrgUnit group = OrgUnit.createUnder(trust, new OrgUnitType("GROUP"), "sunrise-group", "Sunrise Group");
    private final OrgUnit schoolA = OrgUnit.createUnder(group, new OrgUnitType("SCHOOL"), "sunrise-a", "School A");
    private final OrgUnit schoolB = OrgUnit.createUnder(group, new OrgUnitType("SCHOOL"), "sunrise-b", "School B");
    private final OrgUnit clinic = OrgUnit.createUnder(trust, new OrgUnitType("HOSPITAL"), "clinic", "Sunrise Clinic");

    private final InMemoryUserStore users = new InMemoryUserStore();
    private final InMemoryOrgUnitStore units = new InMemoryOrgUnitStore();
    private final OrgAccessService service = new OrgAccessService(users, units);

    @BeforeEach
    void theTree() {
        List.of(trust, group, schoolA, schoolB, clinic).forEach(units::save);
    }

    @Test
    void aTenantWideAdminIsAdminEverywhere() {
        User owner = user("owner@sunrise.org");
        owner.assignRole(RoleCode.ADMIN);

        assertThat(service.accessAt(owner.id(), clinic.id()).roles()).containsExactly("ADMIN", "USER");
        assertThat(service.unitsWithRole(owner.id(), RoleCode.ADMIN)).hasSize(5);
    }

    @Test
    void aPrincipalOfSchoolAIsPrincipalThereOnly() {
        User priya = user("priya@sunrise.org");
        priya.assignRoleAt(PRINCIPAL, schoolA.id());

        assertThat(service.accessAt(priya.id(), schoolA.id()).roles()).containsExactly("PRINCIPAL", "USER");
        assertThat(service.accessAt(priya.id(), schoolB.id()).roles()).containsExactly("USER");
        assertThat(service.accessAt(priya.id(), group.id()).roles()).containsExactly("USER");   // not upwards
        assertThat(service.unitsWithRole(priya.id(), PRINCIPAL)).containsExactly(schoolA.id());
    }

    @Test
    void aGroupAdminIsAdminOfEverySchoolInTheGroupButNotTheClinic() {
        User ravi = user("ravi@sunrise.org");
        ravi.assignRoleAt(RoleCode.ADMIN, group.id());

        assertThat(service.accessAt(ravi.id(), schoolB.id()).roles()).contains("ADMIN");
        assertThat(service.accessAt(ravi.id(), clinic.id()).roles()).doesNotContain("ADMIN");
        assertThat(service.unitsWithRole(ravi.id(), RoleCode.ADMIN))
                .containsExactlyInAnyOrder(group.id(), schoolA.id(), schoolB.id());
    }

    @Test
    void anUnknownUnitIsNotFound() {
        User priya = user("priya@sunrise.org");

        assertThatThrownBy(() -> service.accessAt(priya.id(), OrgUnitId.newId()))
                .isInstanceOf(OrgUnitNotFoundException.class);
    }

    private User user(String email) {
        User user = User.register(DEMO, new Email(email), new PasswordHash("hashed"));
        users.save(user);   // the store keeps the object itself, so later changes to it are "saved" too
        return user;
    }
}
