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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The People registry through the whole app: real security, real tenant filter, real PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PeopleIntegrationTest {

    private static final TenantId DEMO = new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
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
        jdbc.update("DELETE FROM party_person WHERE last_name = 'Test-Rao'");
        jdbc.update("DELETE FROM iam_user WHERE email LIKE 'test-%@orudesk.org'");
        jdbc.update("DELETE FROM iam_tenant WHERE id = ?", ACME.value());
    }

    @Test
    void anAdminRegistersAPersonAndReadsItBack() throws Exception {
        String token = login("test-demo-admin@orudesk.org");
        String id = register(token);

        mockMvc.perform(get("/api/v1/people/{id}", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Asha"))
                .andExpect(jsonPath("$.dateOfBirth").value("1990-04-23"));

        // Nobody sent a tenant id: Hibernate filled it in from the admin's token.
        // UUID.fromString: PostgreSQL won't compare a uuid column with a text parameter.
        UUID tenant = jdbc.queryForObject("SELECT tenant_id FROM party_person WHERE id = ?", UUID.class,
                UUID.fromString(id));
        assertThat(tenant).isEqualTo(DEMO.value());
    }

    @Test
    void anotherTenantCannotSeeThePerson() throws Exception {
        String id = register(login("test-demo-admin@orudesk.org"));

        mockMvc.perform(get("/api/v1/people/{id}", id).header("Authorization", "Bearer " + login("test-acme-admin@orudesk.org")))
                .andExpect(status().isNotFound());
    }

    private String register(String token) throws Exception {
        String json = mockMvc.perform(post("/api/v1/people")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Asha", "lastName": "Test-Rao", "dateOfBirth": "1990-04-23"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
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
