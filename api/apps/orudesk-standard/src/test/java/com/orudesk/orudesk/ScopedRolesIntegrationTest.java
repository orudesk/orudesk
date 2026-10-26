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

import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sunrise Trust: Ravi runs Sunrise Group (two schools); the Clinic is not his. Real tokens, real PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ScopedRolesIntegrationTest {

    private static final TenantId DEMO = new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final UUID DEMO_ROOT = UUID.fromString("00000000-0000-0000-0000-000000000201");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RegisterUserUseCase registerUser;

    @Autowired
    private AssignRoleUseCase assignRole;

    private String owner;
    private UserId ravi;
    private UserId priya;
    private UUID group;
    private UUID schoolA;
    private UUID clinic;

    @BeforeEach
    void sunriseTrust() throws Exception {
        assignRole.assignRole(registerUser.register(
                new RegisterUserCommand(DEMO, new Email("test-owner@orudesk.org"), "test-password")), RoleCode.ADMIN);
        ravi = registerUser.register(new RegisterUserCommand(DEMO, new Email("test-ravi@orudesk.org"), "test-password"));
        priya = registerUser.register(new RegisterUserCommand(DEMO, new Email("test-priya@orudesk.org"), "test-password"));

        owner = login("test-owner@orudesk.org");
        group = createUnit(owner, DEMO_ROOT, "GROUP", "test-group", "Sunrise Group");
        schoolA = createUnit(owner, group, "SCHOOL", "test-school-a", "Sunrise School A");
        clinic = createUnit(owner, DEMO_ROOT, "HOSPITAL", "test-clinic", "Sunrise Clinic");

        // The owner makes Ravi ADMIN of the group, through the API
        mockMvc.perform(post("/api/v1/users/{id}/roles", ravi.value())
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"ADMIN\", \"orgUnitId\": \"" + group + "\"}"))
                .andExpect(status().isNoContent());
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM iam_user WHERE email LIKE 'test-%@orudesk.org'");   // takes their grants with them
        jdbc.update("DELETE FROM iam_org_unit WHERE code LIKE 'test-%' AND parent_id <> ?", DEMO_ROOT);
        jdbc.update("DELETE FROM iam_org_unit WHERE code LIKE 'test-%'");
    }

    @Test
    void raviIsAdminInTheGroupsSchoolsButNotAtTheClinic() throws Exception {
        String token = login("test-ravi@orudesk.org");

        mockMvc.perform(get("/api/v1/org-units/current").header("Authorization", "Bearer " + token)
                        .header("X-Org-Unit", schoolA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sunrise School A"))
                .andExpect(jsonPath("$.roles").value(contains("ADMIN", "USER")));
        mockMvc.perform(get("/api/v1/org-units/current").header("Authorization", "Bearer " + token)
                        .header("X-Org-Unit", clinic.toString()))
                .andExpect(jsonPath("$.roles").value(contains("USER")));
        mockMvc.perform(get("/api/v1/org-units/current").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Org unit missing"));
    }

    @Test
    void raviMayAddASchoolToHisGroupButNotToTheClinic() throws Exception {
        String token = login("test-ravi@orudesk.org");

        createUnit(token, group, "SCHOOL", "test-school-c", "Sunrise School C");
        mockMvc.perform(post("/api/v1/org-units").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unitBody(clinic, "WARD", "test-ward", "Ward 1")))
                .andExpect(status().isForbidden());
    }

    @Test
    void raviMayMakePriyaAdminOfSchoolAButNotOfTheWholeTenant() throws Exception {
        String token = login("test-ravi@orudesk.org");

        mockMvc.perform(post("/api/v1/users/{id}/roles", priya.value()).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"ADMIN\", \"orgUnitId\": \"" + schoolA + "\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/users/{id}/roles", priya.value()).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"ADMIN\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + login("test-priya@orudesk.org")))
                .andExpect(jsonPath("$.roles").value(contains("USER")))
                .andExpect(jsonPath("$.grants[0].role").value("ADMIN"))
                .andExpect(jsonPath("$.grants[0].orgUnitId").value(schoolA.toString()));
    }

    @Test
    void aGroupAdminIsNotATenantAdmin() throws Exception {
        // The People registry is tenant-wide and still needs a tenant-wide ADMIN (the "roles" in the token)
        mockMvc.perform(get("/api/v1/people").header("Authorization", "Bearer " + login("test-ravi@orudesk.org")))
                .andExpect(status().isForbidden());
    }

    private UUID createUnit(String token, UUID parentId, String type, String code, String name) throws Exception {
        String json = mockMvc.perform(post("/api/v1/org-units")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unitBody(parentId, type, code, name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private static String unitBody(UUID parentId, String type, String code, String name) {
        return """
                {"parentId": "%s", "type": "%s", "code": "%s", "name": "%s"}
                """.formatted(parentId, type, code, name);
    }

    private String login(String email) throws Exception {
        String json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"password\": \"test-password\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.accessToken");
    }
}
