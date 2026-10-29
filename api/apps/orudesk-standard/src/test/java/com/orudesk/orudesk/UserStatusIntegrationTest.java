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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Day 48: a disabled user is out at once, not when their access token expires; and back in when enabled. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UserStatusIntegrationTest {

    private static final TenantId DEMO = new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final TenantId ACME = new TenantId(UUID.fromString("00000000-0000-0000-0000-0000000000a3"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RegisterUserUseCase registerUser;

    @Autowired
    private AssignRoleUseCase assignRole;

    private UserId ravi;
    private UserId admin;

    @BeforeEach
    void users() {
        jdbc.update("INSERT INTO iam_tenant (id, code, name) VALUES (?, 'acme-status', 'Acme Status')", ACME.value());
        admin = registerUser.register(new RegisterUserCommand(DEMO, new Email("test-status-admin@orudesk.org"), "admin-password"));
        assignRole.assignRole(admin, RoleCode.ADMIN);
        assignRole.assignRole(registerUser.register(
                new RegisterUserCommand(ACME, new Email("test-status-acme@orudesk.org"), "admin-password")), RoleCode.ADMIN);
        ravi = registerUser.register(new RegisterUserCommand(DEMO, new Email("test-status-ravi@orudesk.org"), "ravi-password"));
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM iam_user WHERE email LIKE 'test-status-%@orudesk.org'");
        jdbc.update("DELETE FROM iam_tenant WHERE id = ?", ACME.value());
    }

    @Test
    void aDisabledUsersTokenStopsWorkingAtOnceAndTheyCannotLogInOrRefresh() throws Exception {
        String raviLogin = login("test-status-ravi@orudesk.org", "ravi-password").andReturn().getResponse().getContentAsString();
        String raviToken = JsonPath.read(raviLogin, "$.accessToken");
        String raviRefresh = JsonPath.read(raviLogin, "$.refreshToken");
        me(raviToken).andExpect(status().isOk());

        String adminToken = token("test-status-admin@orudesk.org", "admin-password");
        mockMvc.perform(post("/api/v1/users/{id}/disable", ravi.value()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        me(raviToken).andExpect(status().isUnauthorized());   // the same token, a second later: refused
        login("test-status-ravi@orudesk.org", "ravi-password").andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + raviRefresh + "\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users").param("q", "test-status-ravi").header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.items[0].enabled").value(false));

        mockMvc.perform(post("/api/v1/users/{id}/enable", ravi.value()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        me(token("test-status-ravi@orudesk.org", "ravi-password")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/audit-entries").param("subjectId", ravi.value().toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.items[0].action").value("identity.user.enabled"))
                .andExpect(jsonPath("$.items[1].action").value("identity.user.disabled"));
    }

    @Test
    void anAdminCannotDisableThemselves() throws Exception {
        mockMvc.perform(post("/api/v1/users/{id}/disable", admin.value())
                        .header("Authorization", "Bearer " + token("test-status-admin@orudesk.org", "admin-password")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Not allowed on your own account"));
    }

    @Test
    void anotherTenantsAdminCannotDisableTheUser() throws Exception {
        mockMvc.perform(post("/api/v1/users/{id}/disable", ravi.value())
                        .header("Authorization", "Bearer " + token("test-status-acme@orudesk.org", "admin-password")))
                .andExpect(status().isNotFound());
        login("test-status-ravi@orudesk.org", "ravi-password").andExpect(status().isOk());
    }

    @Test
    void aUserWhoIsNotAnAdminCannotDisableAnyone() throws Exception {
        mockMvc.perform(post("/api/v1/users/{id}/disable", admin.value())
                        .header("Authorization", "Bearer " + token("test-status-ravi@orudesk.org", "ravi-password")))
                .andExpect(status().isForbidden());
    }

    private ResultActions me(String token) throws Exception {
        return mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}"));
    }

    private String token(String email, String password) throws Exception {
        return JsonPath.read(login(email, password).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.accessToken");
    }
}
