package com.orudesk.orudesk;

import com.orudesk.orudesk.identity.application.port.in.RegisterUserCommand;
import com.orudesk.orudesk.identity.application.port.in.RegisterUserUseCase;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.TenantId;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.AfterEach;
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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Day 43 through the whole app: the login limit in real Redis, and the headers every response carries. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LoginHardeningTest {

    private static final TenantId DEMO = new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RegisterUserUseCase registerUser;

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM iam_user WHERE email LIKE 'test-hardening-%@orudesk.org'");
    }

    @Test
    void fiveWrongPasswordsBlockTheEmailEvenForTheRightOne() throws Exception {
        // A new address each run: Redis keeps the count for 15 minutes, across test runs
        String email = "test-hardening-" + UUID.randomUUID() + "@orudesk.org";
        registerUser.register(new RegisterUserCommand(DEMO, new Email(email), "right-password"));

        for (int i = 0; i < 5; i++) {
            login(email, "wrong-password").andExpect(status().isUnauthorized());
        }

        login(email, "right-password")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", hasSecondsUpTo(15 * 60)));
    }

    @Test
    void anotherEmailIsNotAffected() throws Exception {
        String blocked = "test-hardening-" + UUID.randomUUID() + "@orudesk.org";
        String other = "test-hardening-" + UUID.randomUUID() + "@orudesk.org";
        registerUser.register(new RegisterUserCommand(DEMO, new Email(other), "right-password"));
        for (int i = 0; i < 5; i++) {
            login(blocked, "wrong-password").andExpect(status().isUnauthorized());   // nobody has this address
        }

        login(blocked, "any-password").andExpect(status().isTooManyRequests());
        login(other, "right-password").andExpect(status().isOk());
    }

    @Test
    void apiResponsesForbidEverythingAPageCouldDo() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));

        // Swagger UI is a page with scripts: our strict policy would break it
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(header().doesNotExist("Content-Security-Policy"));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}"));
    }

    private static Matcher<String> hasSecondsUpTo(int max) {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(String value) {
                return allOf(greaterThan(0), lessThanOrEqualTo(max)).matches(Integer.parseInt(value));
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a number of seconds between 1 and " + max);
            }
        };
    }
}
