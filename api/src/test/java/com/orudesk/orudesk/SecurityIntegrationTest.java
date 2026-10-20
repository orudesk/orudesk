package com.orudesk.orudesk;

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
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole app, real security, real MariaDB: users log in with HTTP Basic.
 * @Transactional rolls everything back after each test.
 */
@SpringBootTest
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
    void assigningARoleWithoutLoggingInGives401() throws Exception {
        mockMvc.perform(post("/api/v1/users/{id}/roles", plainUser.value())
                        .contentType(MediaType.APPLICATION_JSON).content(ROLE_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aWrongPasswordGives401() throws Exception {
        mockMvc.perform(post("/api/v1/users/{id}/roles", plainUser.value())
                        .with(httpBasic("test-admin@orudesk.org", "wrong-password"))
                        .contentType(MediaType.APPLICATION_JSON).content(ROLE_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aUserWhoIsNotAnAdminGets403() throws Exception {
        mockMvc.perform(post("/api/v1/users/{id}/roles", plainUser.value())
                        .with(httpBasic("test-user@orudesk.org", "user-password"))
                        .contentType(MediaType.APPLICATION_JSON).content(ROLE_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void anAdminCanAssignARole() throws Exception {
        mockMvc.perform(post("/api/v1/users/{id}/roles", plainUser.value())
                        .with(httpBasic("test-admin@orudesk.org", "admin-password"))
                        .contentType(MediaType.APPLICATION_JSON).content(ROLE_JSON))
                .andExpect(status().isNoContent());
    }
}
