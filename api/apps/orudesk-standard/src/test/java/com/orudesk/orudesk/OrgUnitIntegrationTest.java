package com.orudesk.orudesk;

import com.jayway.jsonpath.JsonPath;
import com.orudesk.orudesk.identity.application.port.in.AssignRoleUseCase;
import com.orudesk.orudesk.identity.application.port.in.RegisterUserCommand;
import com.orudesk.orudesk.identity.application.port.in.RegisterUserUseCase;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.TenantId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Org units through the whole app, and the database rules that back them up. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrgUnitIntegrationTest {

    private static final TenantId DEMO = new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final UUID DEMO_ROOT = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final TenantId ACME = new TenantId(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RegisterUserUseCase registerUser;

    @Autowired
    private AssignRoleUseCase assignRole;

    @BeforeEach
    void anAdminInEachTenant() {
        jdbc.update("INSERT INTO iam_tenant (id, code, name) VALUES (?, 'acme', 'Acme Hospital')", ACME.value());
        assignRole.assignRole(registerUser.register(
                new RegisterUserCommand(DEMO, new Email("test-demo-admin@orudesk.org"), "admin-password")), RoleCode.ADMIN);
        assignRole.assignRole(registerUser.register(
                new RegisterUserCommand(ACME, new Email("test-acme-admin@orudesk.org"), "admin-password")), RoleCode.ADMIN);
    }

    @AfterEach
    void cleanUp() {
        // Children first: the parent foreign key refuses to delete a unit that still has units below it
        jdbc.update("DELETE FROM iam_org_unit WHERE code LIKE 'test-%' AND parent_id <> ?", DEMO_ROOT);
        jdbc.update("DELETE FROM iam_org_unit WHERE code LIKE 'test-%'");
        jdbc.update("DELETE FROM iam_user WHERE email LIKE 'test-%@orudesk.org'");
        jdbc.update("DELETE FROM iam_org_unit WHERE tenant_id = ?", ACME.value());
        jdbc.update("DELETE FROM iam_tenant WHERE id = ?", ACME.value());
    }

    @Test
    void anAdminBuildsAGroupWithTwoSchools() throws Exception {
        String token = login("test-demo-admin@orudesk.org");
        String group = create(token, DEMO_ROOT, "GROUP", "test-group", "Sunrise Group");
        create(token, UUID.fromString(group), "SCHOOL", "test-school-b", "Sunrise School B");
        create(token, UUID.fromString(group), "SCHOOL", "test-school-a", "Sunrise School A");

        mockMvc.perform(get("/api/v1/org-units").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("demo"))
                .andExpect(jsonPath("$.children[0].name").value("Sunrise Group"))
                .andExpect(jsonPath("$.children[0].children[0].name").value("Sunrise School A"))
                .andExpect(jsonPath("$.children[0].children[1].name").value("Sunrise School B"));
    }

    @Test
    void anotherTenantCannotHangAUnitUnderOurTree() throws Exception {
        mockMvc.perform(post("/api/v1/org-units")
                        .header("Authorization", "Bearer " + login("test-acme-admin@orudesk.org"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(DEMO_ROOT, "SCHOOL", "test-sneaky", "Sneaky School")))
                .andExpect(status().isNotFound());
    }

    @Test
    void theDatabaseRefusesACrossTenantParentEvenIfTheCodeForgetsToCheck() {
        // The application already said 404 above. This is the second lock: the composite foreign key.
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO iam_org_unit (id, tenant_id, parent_id, type, code, name) VALUES (?, ?, ?, 'SCHOOL', 'test-x', 'X')",
                UUID.randomUUID(), ACME.value(), DEMO_ROOT))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_iam_org_unit_parent");
    }

    @Test
    void aTenantHasOnlyOneRoot() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO iam_org_unit (id, tenant_id, parent_id, type, code, name) VALUES (?, ?, NULL, 'ORGANISATION', 'test-root2', 'Second root')",
                UUID.randomUUID(), DEMO.value()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ux_iam_org_unit_one_root");
    }

    private String create(String token, UUID parentId, String type, String code, String name) throws Exception {
        String json = mockMvc.perform(post("/api/v1/org-units")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(parentId, type, code, name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private static String body(UUID parentId, String type, String code, String name) {
        return """
                {"parentId": "%s", "type": "%s", "code": "%s", "name": "%s"}
                """.formatted(parentId, type, code, name);
    }

    private String login(String email) throws Exception {
        String json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"password\": \"admin-password\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.accessToken");
    }
}
