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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The audit log through the whole app: a change made over HTTP shows up with who, when and which request. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuditIntegrationTest {

    private static final TenantId DEMO = new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final TenantId ACME = new TenantId(UUID.fromString("00000000-0000-0000-0000-0000000000a2"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RegisterUserUseCase registerUser;

    @Autowired
    private AssignRoleUseCase assignRole;

    @BeforeEach
    void users() {
        jdbc.update("INSERT INTO iam_tenant (id, code, name) VALUES (?, 'acme-audit', 'Acme Audit')", ACME.value());
        assignRole.assignRole(registerUser.register(
                new RegisterUserCommand(DEMO, new Email("test-audit-admin@orudesk.org"), "admin-password")), RoleCode.ADMIN);
        assignRole.assignRole(registerUser.register(
                new RegisterUserCommand(ACME, new Email("test-audit-acme@orudesk.org"), "admin-password")), RoleCode.ADMIN);
        registerUser.register(new RegisterUserCommand(DEMO, new Email("test-audit-user@orudesk.org"), "user-password"));
    }

    @AfterEach
    void cleanUp() {
        // Audit entries stay: the log is append-only, and the tests look only at their own subjects
        jdbc.update("DELETE FROM party_person WHERE last_name = 'Test-Audit'");
        jdbc.update("DELETE FROM iam_user WHERE email LIKE 'test-audit-%@orudesk.org'");
        jdbc.update("DELETE FROM iam_tenant WHERE id = ?", ACME.value());
    }

    @Test
    void registeringAPersonIsRecordedWithWhoAndWhichRequest() throws Exception {
        String token = login("test-audit-admin@orudesk.org", "admin-password");
        String json = mockMvc.perform(post("/api/v1/people")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Request-Id", "audit-test-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\": \"Meera\", \"lastName\": \"Test-Audit\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String personId = JsonPath.read(json, "$.id");

        mockMvc.perform(get("/api/v1/audit-entries").param("subjectId", personId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].action").value("party.person.registered"))
                .andExpect(jsonPath("$.items[0].summary").value("Registered Meera Test-Audit"))
                .andExpect(jsonPath("$.items[0].actorEmail").value("test-audit-admin@orudesk.org"))
                .andExpect(jsonPath("$.items[0].requestId").value("audit-test-0001"));
    }

    @Test
    void aRoleGivenAtSignUpTimeIsRecordedInTheUsersTenantEvenWithNobodyLoggedIn() throws Exception {
        UUID acmeAdmin = jdbc.queryForObject("SELECT id FROM iam_user WHERE email = 'test-audit-acme@orudesk.org'", UUID.class);

        String token = login("test-audit-acme@orudesk.org", "admin-password");
        mockMvc.perform(get("/api/v1/audit-entries").param("subjectId", acmeAdmin.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].action").value(contains(
                        "identity.role.granted", "identity.user.registered")))
                .andExpect(jsonPath("$.items[0].actorId").value(nullValue()));   // nobody was logged in

        // ...and another tenant's admin sees none of it
        String demoToken = login("test-audit-admin@orudesk.org", "admin-password");
        mockMvc.perform(get("/api/v1/audit-entries").param("subjectId", acmeAdmin.toString())
                        .header("Authorization", "Bearer " + demoToken))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void onlyTenantAdminsReadTheLog() throws Exception {
        mockMvc.perform(get("/api/v1/audit-entries")
                        .header("Authorization", "Bearer " + login("test-audit-user@orudesk.org", "user-password")))
                .andExpect(status().isForbidden());
    }

    @Test
    void theDatabaseRefusesToChangeHistory() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_entry", Integer.class)).isPositive();

        assertThatThrownBy(() -> jdbc.update("UPDATE audit_entry SET summary = 'nothing happened'"))
                .hasMessageContaining("audit entries cannot be changed or deleted");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM audit_entry"))
                .hasMessageContaining("audit entries cannot be changed or deleted");
    }

    private String login(String email, String password) throws Exception {
        String json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.accessToken");
    }
}
