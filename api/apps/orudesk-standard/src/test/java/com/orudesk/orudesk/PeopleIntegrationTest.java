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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Test
    void searchPagesThroughMatchesInNameOrderAndStaysInsideTheTenant() throws Exception {
        String demo = login("test-demo-admin@orudesk.org");
        for (String first : List.of("Chitra", "Asha", "Bina")) {
            registerNamed(demo, first, "Test-Rao");
        }
        registerNamed(login("test-acme-admin@orudesk.org"), "Deepa", "Test-Rao");   // another tenant

        String page1 = mockMvc.perform(get("/api/v1/people").param("q", "test-rao").param("size", "2")
                        .header("Authorization", "Bearer " + demo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].firstName").value(contains("Asha", "Bina")))
                .andReturn().getResponse().getContentAsString();
        String cursor = JsonPath.read(page1, "$.nextCursor");

        mockMvc.perform(get("/api/v1/people").param("q", "test-rao").param("size", "2").param("after", cursor)
                        .header("Authorization", "Bearer " + demo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].firstName").value(contains("Chitra")))   // never Deepa
                .andExpect(jsonPath("$.nextCursor").value(nullValue()));
    }

    @Test
    void wildcardsInTheSearchTextAreTakenLiterally() throws Exception {
        String token = login("test-demo-admin@orudesk.org");
        registerNamed(token, "Asha", "Test-Rao");

        mockMvc.perform(get("/api/v1/people").param("q", "t_st").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void twoAdminsEditTheSamePersonAndTheSecondIsToldToReload() throws Exception {
        String token = login("test-demo-admin@orudesk.org");
        String id = register(token);
        mockMvc.perform(get("/api/v1/people/{id}", id).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.version").value(0));

        // Both opened the form at version 0. The first save wins and moves the person to version 1...
        mockMvc.perform(put("/api/v1/people/{id}", id).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version": 0, "firstName": "Asha", "lastName": "Test-Rao", "phone": "+91 98450 11111"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.dateOfBirth").value(nullValue()));   // PUT replaces: left out means cleared

        // ...so the second, still at version 0, would undo the phone number. It's refused instead.
        mockMvc.perform(put("/api/v1/people/{id}", id).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version": 0, "firstName": "Asha", "lastName": "Test-Rao", "email": "asha@example.org"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.currentVersion").value(1));

        assertThat(jdbc.queryForObject("SELECT phone FROM party_person WHERE id = ?", String.class, UUID.fromString(id)))
                .isEqualTo("+91 98450 11111");
        // The audit log says who changed what, from what to what (Day 47)
        mockMvc.perform(get("/api/v1/audit-entries").param("subjectId", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].summary").value("Changed dateOfBirth, phone of Asha Test-Rao"))
                .andExpect(jsonPath("$.items[0].changes[0].field").value("dateOfBirth"))
                .andExpect(jsonPath("$.items[0].changes[0].before").value("1990-04-23"))
                .andExpect(jsonPath("$.items[0].changes[0].after").value(nullValue()))
                .andExpect(jsonPath("$.items[1].action").value("party.person.registered"))
                .andExpect(jsonPath("$.items[1].changes").isEmpty());
    }

    @Test
    void anotherTenantCannotEditThePerson() throws Exception {
        String id = register(login("test-demo-admin@orudesk.org"));

        mockMvc.perform(put("/api/v1/people/{id}", id).header("Authorization", "Bearer " + login("test-acme-admin@orudesk.org"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\": 0, \"firstName\": \"Mallory\", \"lastName\": \"Test-Rao\"}"))
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

    private void registerNamed(String token, String firstName, String lastName) throws Exception {
        mockMvc.perform(post("/api/v1/people")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\": \"" + firstName + "\", \"lastName\": \"" + lastName + "\"}"))
                .andExpect(status().isCreated());
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
