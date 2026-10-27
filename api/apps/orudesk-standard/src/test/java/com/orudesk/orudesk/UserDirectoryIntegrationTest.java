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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The user list, the role list and taking roles away, through the whole app against PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UserDirectoryIntegrationTest {

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
    void usersInTwoTenants() {
        jdbc.update("INSERT INTO iam_tenant (id, code, name) VALUES (?, 'acme-dir', 'Acme')", ACME.value());
        assignRole.assignRole(register(DEMO, "test-dir-admin@orudesk.org"), RoleCode.ADMIN);
        for (int i = 1; i <= 4; i++) {
            register(DEMO, "test-dir-" + i + "@orudesk.org");
        }
        register(DEMO, "test-dir_under@orudesk.org");
        register(ACME, "test-dir-acme@orudesk.org");
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM iam_user WHERE email LIKE 'test-dir%@orudesk.org'");
        jdbc.update("DELETE FROM iam_tenant WHERE id = ?", ACME.value());
    }

    @Test
    void anAdminPagesThroughTheTenantsUsersSortedByEmail() throws Exception {
        String token = login("test-dir-admin@orudesk.org");
        List<String> emails = new ArrayList<>();
        String after = null;
        int pages = 0;
        do {
            var request = get("/api/v1/users").param("q", "test-dir").param("size", "2")
                    .header("Authorization", "Bearer " + token);
            if (after != null) {
                request.param("after", after);
            }
            String json = mockMvc.perform(request).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            emails.addAll(JsonPath.read(json, "$.items[*].email"));
            after = JsonPath.read(json, "$.nextCursor");
            pages++;
        } while (after != null);

        // Six users in this tenant, in email order; the other tenant's user is not there
        assertThat(emails).containsExactly(
                "test-dir-1@orudesk.org", "test-dir-2@orudesk.org", "test-dir-3@orudesk.org",
                "test-dir-4@orudesk.org", "test-dir-admin@orudesk.org", "test-dir_under@orudesk.org");
        assertThat(pages).isEqualTo(3);
    }

    @Test
    void eachUserComesWithTheirGrants() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("q", "test-dir-admin")
                        .header("Authorization", "Bearer " + login("test-dir-admin@orudesk.org")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].email").value("test-dir-admin@orudesk.org"))
                .andExpect(jsonPath("$.items[0].enabled").value(true))
                .andExpect(jsonPath("$.items[0].grants[*].role").value(contains("ADMIN", "USER")));
    }

    @Test
    void anUnderscoreInTheSearchIsJustAnUnderscore() throws Exception {
        // Unescaped, LIKE '%dir_%' would match every "dir-" address too
        mockMvc.perform(get("/api/v1/users").param("q", "dir_")
                        .header("Authorization", "Bearer " + login("test-dir-admin@orudesk.org")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].email").value("test-dir_under@orudesk.org"));
    }

    @Test
    void aUserWhoIsNotAnAdminMayNotListUsersButMaySeeTheRoles() throws Exception {
        String token = login("test-dir-1@orudesk.org");

        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/roles").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'ADMIN')].name").isNotEmpty());
    }

    @Test
    void aRevokedRoleIsGoneFromTheUsersProfileAtOnce() throws Exception {
        UserId anna = register(DEMO, "test-dir-anna@orudesk.org");
        assignRole.assignRole(anna, RoleCode.ADMIN);
        String annasToken = login("test-dir-anna@orudesk.org");
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + annasToken))   // now cached
                .andExpect(jsonPath("$.grants.length()").value(2));

        mockMvc.perform(delete("/api/v1/users/{id}/roles/{role}", anna.value(), "ADMIN")
                        .header("Authorization", "Bearer " + login("test-dir-admin@orudesk.org")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + annasToken))
                .andExpect(jsonPath("$.grants[*].role").value(contains("USER")));
    }

    @Test
    void anAdminCannotLockThemselfOut() throws Exception {
        String token = login("test-dir-admin@orudesk.org");
        String me = JsonPath.read(mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(delete("/api/v1/users/{id}/roles/{role}", me, "ADMIN").header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Role change not allowed"));
    }

    private UserId register(TenantId tenant, String email) {
        return registerUser.register(new RegisterUserCommand(tenant, new Email(email), "user-password"));
    }

    private String login(String email) throws Exception {
        String json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"password\": \"user-password\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.accessToken");
    }
}
