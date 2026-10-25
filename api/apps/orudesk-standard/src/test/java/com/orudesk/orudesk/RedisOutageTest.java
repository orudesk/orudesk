package com.orudesk.orudesk;

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
import org.testcontainers.containers.GenericContainer;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * What the API does when Redis is down: login answers 503 (refresh tokens live in Redis, so it really can't work),
 * but /me still answers from PostgreSQL (the cache is optional).
 *
 * The property gives this test its own Spring context, and so its own containers:
 * stopping Redis here can't break the other tests.
 */
@SpringBootTest(properties = "spring.data.redis.timeout=500ms")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RedisOutageTest {

    private static final TenantId DEMO = new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GenericContainer<?> redis;

    @Autowired
    private RegisterUserUseCase registerUser;

    @Autowired
    private JdbcTemplate jdbc;

    private UserId userId;

    @BeforeEach
    void aUserAndNoRedis() throws Exception {
        userId = registerUser.register(new RegisterUserCommand(DEMO, new Email("test-outage@orudesk.org"), "user-password"));
        if (redis.isRunning()) {   // only before the first test: once stopped, it stays stopped
            login().andExpect(status().isOk());   // the app is now connected to Redis, as it is in real life
            redis.stop();
        }
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM iam_user WHERE email LIKE 'test-%@orudesk.org'");
    }

    @Test
    void loginSays503AndWhenToRetry() throws Exception {
        login()
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(jsonPath("$.title").value("Service unavailable"));
    }

    @Test
    void theProfileStillWorksWithoutTheCache() throws Exception {
        mockMvc.perform(get("/api/v1/users/me").with(jwt().jwt(token -> token
                        .subject(userId.value().toString())
                        .claim("tenant_id", DEMO.value().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test-outage@orudesk.org"));
    }

    private ResultActions login() throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "test-outage@orudesk.org", "password": "user-password"}
                        """));
    }
}
