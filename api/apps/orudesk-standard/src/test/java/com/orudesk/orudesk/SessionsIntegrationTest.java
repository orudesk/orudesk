package com.orudesk.orudesk;

import com.jayway.jsonpath.JsonPath;
import com.orudesk.orudesk.identity.application.port.in.RegisterUserCommand;
import com.orudesk.orudesk.identity.application.port.in.RegisterUserUseCase;
import com.orudesk.orudesk.identity.domain.Email;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Day 66: a user's sessions, real Redis and PostgreSQL. Ending one ends its tokens at once, access ones too. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SessionsIntegrationTest {

    private static final TenantId DEMO = new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final String FIREFOX = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0";
    private static final String PHONE = "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) "
            + "Chrome/141.0.0.0 Mobile Safari/537.36";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RegisterUserUseCase registerUser;

    private UserId asha;

    @BeforeEach
    void user() {
        asha = registerUser.register(new RegisterUserCommand(DEMO, new Email("test-sessions-asha@orudesk.org"), "asha-password"));
        registerUser.register(new RegisterUserCommand(DEMO, new Email("test-sessions-ravi@orudesk.org"), "ravi-password"));
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM iam_user WHERE email LIKE 'test-sessions-%@orudesk.org'");
    }

    @Test
    void theUserSeesWhereTheyAreSignedInAndEndsTheOtherSessionAtOnce() throws Exception {
        Tokens laptop = login("test-sessions-asha@orudesk.org", "asha-password", FIREFOX);
        Tokens phone = login("test-sessions-asha@orudesk.org", "asha-password", PHONE);

        String list = mockMvc.perform(get("/api/v1/users/me/sessions").header("Authorization", "Bearer " + laptop.access()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].device").value("Chrome on Android"))   // most recently used first
                .andExpect(jsonPath("$[0].current").value(false))
                .andExpect(jsonPath("$[1].device").value("Firefox on Windows"))
                .andExpect(jsonPath("$[1].current").value(true))
                .andReturn().getResponse().getContentAsString();
        String phoneSession = JsonPath.read(list, "$[0].id");

        mockMvc.perform(delete("/api/v1/users/me/sessions/{id}", phoneSession)
                        .header("Authorization", "Bearer " + laptop.access()))
                .andExpect(status().isNoContent());

        me(phone.access()).andExpect(status().isUnauthorized());   // the phone's access token, at once
        refresh(phone.refresh()).andExpect(status().isUnauthorized());
        me(laptop.access()).andExpect(status().isOk());             // the laptop carries on
        mockMvc.perform(get("/api/v1/users/me/sessions").header("Authorization", "Bearer " + laptop.access()))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void aRefreshKeepsTheSessionAndLogoutEndsItsAccessTokenToo() throws Exception {
        Tokens first = login("test-sessions-asha@orudesk.org", "asha-password", FIREFOX);
        String refreshed = refresh(first.refresh()).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Tokens second = new Tokens(JsonPath.read(refreshed, "$.accessToken"), JsonPath.read(refreshed, "$.refreshToken"));

        mockMvc.perform(get("/api/v1/users/me/sessions").header("Authorization", "Bearer " + second.access()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].current").value(true));
        me(first.access()).andExpect(status().isOk());   // same session: the older access token lives out its time

        mockMvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + second.refresh() + "\"}"))
                .andExpect(status().isNoContent());

        me(second.access()).andExpect(status().isUnauthorized());
        me(first.access()).andExpect(status().isUnauthorized());
    }

    @Test
    void signingOutEverywhereElseKeepsOnlyThisSessionAndIsAudited() throws Exception {
        Tokens here = login("test-sessions-asha@orudesk.org", "asha-password", FIREFOX);
        List<Tokens> others = List.of(login("test-sessions-asha@orudesk.org", "asha-password", PHONE),
                login("test-sessions-asha@orudesk.org", "asha-password", "curl/8.5.0"));

        mockMvc.perform(post("/api/v1/users/me/sessions/end-others").header("Authorization", "Bearer " + here.access()))
                .andExpect(status().isNoContent());

        for (Tokens other : others) {
            me(other.access()).andExpect(status().isUnauthorized());
        }
        me(here.access()).andExpect(status().isOk());
        Integer audited = jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_entry WHERE subject_id = ? AND action = 'identity.sessions.ended'"
                        + " AND summary LIKE '%out of 2 sessions'", Integer.class, asha.value());
        assertThat(audited).isEqualTo(1);
    }

    @Test
    void nobodyCanEndSomeoneElsesSession() throws Exception {
        Tokens asha = login("test-sessions-asha@orudesk.org", "asha-password", FIREFOX);
        Tokens ravi = login("test-sessions-ravi@orudesk.org", "ravi-password", FIREFOX);
        String list = mockMvc.perform(get("/api/v1/users/me/sessions").header("Authorization", "Bearer " + asha.access()))
                .andReturn().getResponse().getContentAsString();
        String ashaSession = JsonPath.read(list, "$[0].id");

        mockMvc.perform(delete("/api/v1/users/me/sessions/{id}", ashaSession).header("Authorization", "Bearer " + ravi.access()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Session not found"));
        me(asha.access()).andExpect(status().isOk());
    }

    private record Tokens(String access, String refresh) {
    }

    private Tokens login(String email, String password, String userAgent) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .header("User-Agent", userAgent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new Tokens(JsonPath.read(body, "$.accessToken"), JsonPath.read(body, "$.refreshToken"));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\": \"" + refreshToken + "\"}"));
    }

    private ResultActions me(String token) throws Exception {
        return mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token));
    }
}
