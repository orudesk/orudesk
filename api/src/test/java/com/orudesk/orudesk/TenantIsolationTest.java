package com.orudesk.orudesk;

import com.jayway.jsonpath.JsonPath;
import com.orudesk.orudesk.identity.application.port.in.AssignRoleUseCase;
import com.orudesk.orudesk.identity.application.port.in.RegisterUserCommand;
import com.orudesk.orudesk.identity.application.port.in.RegisterUserUseCase;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.TenantId;
import com.orudesk.orudesk.identity.domain.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Two organisations in one database. An ADMIN of one must not even see the users of the other.
 * This is the test the tenancy ADR promised.
 *
 * Not @Transactional on purpose: a test transaction would open one Hibernate session before the request,
 * with no logged-in user, and the request would reuse it, so the tenant filter would never switch on.
 * Instead, @AfterEach deletes what the test created.
 */

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
class TenantIsolationTest {

    private static final TenantId DEMO = new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final TenantId ACME = new TenantId(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));
    private static final String ROLE_JSON = """
            {"role": "ADMIN"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RegisterUserUseCase registerUser;

    @Autowired
    private AssignRoleUseCase assignRole;

    private UserId demoUser;
    private UserId acmeUser;

    @BeforeEach
    void twoTenantsWithAnAdminEach() {
        jdbc.update("INSERT INTO iam_tenant (id, code, name) VALUES (?, 'acme', 'Acme Hospital')", ACME.value());

        assignRole.assignRole(registerUser.register(
                new RegisterUserCommand(DEMO, new Email("test-demo-admin@orudesk.org"), "admin-password")), RoleCode.ADMIN);
        assignRole.assignRole(registerUser.register(
                new RegisterUserCommand(ACME, new Email("test-acme-admin@orudesk.org"), "admin-password")), RoleCode.ADMIN);

        demoUser = registerUser.register(new RegisterUserCommand(DEMO, new Email("test-demo-user@orudesk.org"), "user-password"));
        acmeUser = registerUser.register(new RegisterUserCommand(ACME, new Email("test-acme-user@orudesk.org"), "user-password"));
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM iam_user WHERE email LIKE 'test-%@orudesk.org'");
        jdbc.update("DELETE FROM iam_tenant WHERE id = ?", ACME.value());
    }

    @Test
    void anAdminCanManageUsersOfTheirOwnTenant() throws Exception {
        String token = login("test-acme-admin@orudesk.org");

        mockMvc.perform(post("/api/v1/users/{id}/roles", acmeUser.value())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(ROLE_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    void anAdminCannotSeeUsersOfAnotherTenant() throws Exception {
        String token = login("test-acme-admin@orudesk.org");

        // 404, not 403: for Acme, the Demo user simply doesn't exist. We don't even confirm the id is real.
        mockMvc.perform(post("/api/v1/users/{id}/roles", demoUser.value())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(ROLE_JSON))
                .andExpect(status().isNotFound());
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
