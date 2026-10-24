package com.orudesk.orudesk;

import com.jayway.jsonpath.JsonPath;
import com.orudesk.orudesk.identity.application.port.in.AssignRoleUseCase;
import com.orudesk.orudesk.identity.application.port.in.RegisterUserCommand;
import com.orudesk.orudesk.identity.application.port.in.RegisterUserUseCase;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.TenantId;
import com.orudesk.orudesk.identity.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.context.annotation.Import;

/**
 * The whole app, real security, real database and Redis: users log in and send a JWT.
 * @Transactional rolls the database back after each test (refresh tokens in Redis expire by themselves).
 */


@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTest {

    private static final TenantId DEMO = new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final String ROLE_JSON = """
            {"role": "ADMIN"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegisterUserUseCase registerUser;

    @Autowired
    private AssignRoleUseCase assignRole;

    private UserId plainUser;

    @BeforeEach
    void createUsers() {
        UserId admin = registerUser.register(
                new RegisterUserCommand(DEMO, new Email("test-admin@orudesk.org"), "admin-password"));
        assignRole.assignRole(admin, RoleCode.ADMIN);
        plainUser = registerUser.register(
                new RegisterUserCommand(DEMO, new Email("test-user@orudesk.org"), "user-password"));
    }

    @Test
    void anyoneCanRegister() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "test-new@orudesk.org", "password": "new-password"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void assigningARoleWithoutATokenGives401() throws Exception {
        mockMvc.perform(post("/api/v1/users/{id}/roles", plainUser.value())
                        .contentType(MediaType.APPLICATION_JSON).content(ROLE_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aForgedTokenGives401() throws Exception {
        mockMvc.perform(post("/api/v1/users/{id}/roles", plainUser.value())
                        .header("Authorization", "Bearer not.a.real-token")
                        .contentType(MediaType.APPLICATION_JSON).content(ROLE_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aWrongPasswordGives401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "test-admin@orudesk.org", "password": "wrong-password"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aUserWhoIsNotAnAdminGets403() throws Exception {
        String token = accessToken(login("test-user@orudesk.org", "user-password"));

        mockMvc.perform(post("/api/v1/users/{id}/roles", plainUser.value())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(ROLE_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void anAdminCanAssignARole() throws Exception {
        String token = accessToken(login("test-admin@orudesk.org", "admin-password"));

        mockMvc.perform(post("/api/v1/users/{id}/roles", plainUser.value())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(ROLE_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    void aRefreshTokenCanBeUsedOnlyOnce() throws Exception {
        String refreshToken = refreshToken(login("test-user@orudesk.org", "user-password"));

        refresh(refreshToken).andExpect(status().isOk());
        refresh(refreshToken).andExpect(status().isUnauthorized());
    }

    @Test
    void afterLogoutTheRefreshTokenNoLongerWorks() throws Exception {
        String refreshToken = refreshToken(login("test-user@orudesk.org", "user-password"));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                .andExpect(status().isNoContent());

        refresh(refreshToken).andExpect(status().isUnauthorized());
    }

    private String login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\": \"" + refreshToken + "\"}"));
    }

    private static String accessToken(String loginJson) {
        return JsonPath.read(loginJson, "$.accessToken");
    }

    private static String refreshToken(String loginJson) {
        return JsonPath.read(loginJson, "$.refreshToken");
    }
}
