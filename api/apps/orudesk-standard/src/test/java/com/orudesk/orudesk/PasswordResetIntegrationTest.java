package com.orudesk.orudesk;

import com.jayway.jsonpath.JsonPath;
import com.orudesk.orudesk.identity.application.port.in.RegisterUserCommand;
import com.orudesk.orudesk.identity.application.port.in.RegisterUserUseCase;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.TenantId;
import com.orudesk.orudesk.shared.EmailMessage;
import com.orudesk.orudesk.shared.RecordingEmailSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Day 53: "I forgot my password", from the request to logging in with the new one, against PostgreSQL and Redis. */
@SpringBootTest(properties = "orudesk.web.base-url=https://erp.example.org")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, PasswordResetIntegrationTest.Mail.class})
class PasswordResetIntegrationTest {

    private static final TenantId DEMO = new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final String ASHA = "test-reset-asha@orudesk.org";
    private static final Pattern LINK = Pattern.compile("https://erp\\.example\\.org/reset-password\\?token=([A-Za-z0-9_-]+)");

    @TestConfiguration(proxyBeanMethods = false)
    static class Mail {
        @Bean
        @Primary
        RecordingEmailSender recordingEmailSender() {
            return new RecordingEmailSender();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RegisterUserUseCase registerUser;

    @Autowired
    private RecordingEmailSender mail;

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM iam_user WHERE email LIKE 'test-reset-%@orudesk.org'");   // tokens go with them
    }

    @Test
    void theLinkSetsANewPasswordAndEndsTheOldSessions() throws Exception {
        registerUser.register(new RegisterUserCommand(DEMO, new Email(ASHA), "old-password"));
        String oldRefreshToken = JsonPath.read(login(ASHA, "old-password").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.refreshToken");

        requestReset(ASHA).andExpect(status().isAccepted());
        String token = tokenFromEmail(ASHA);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM iam_password_reset WHERE token_hash = ?", Integer.class, token))
                .as("only the hash is stored").isZero();

        confirm(token, "new-password").andExpect(status().isNoContent());

        login(ASHA, "old-password").andExpect(status().isUnauthorized());
        login(ASHA, "new-password").andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + oldRefreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
        confirm(token, "third-password")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid reset link"));
    }

    @Test
    void anUnknownEmailGetsTheSameAnswerAndNoEmail() throws Exception {
        requestReset("test-reset-nobody@orudesk.org").andExpect(status().isAccepted());

        // The known-email case has had time to send by now; give this one the same chance to (wrongly) do so
        registerUser.register(new RegisterUserCommand(DEMO, new Email("test-reset-ravi@orudesk.org"), "ravi-password"));
        requestReset("test-reset-ravi@orudesk.org").andExpect(status().isAccepted());
        tokenFromEmail("test-reset-ravi@orudesk.org");
        assertThat(mail.sentTo("test-reset-nobody@orudesk.org")).isEmpty();
    }

    @Test
    void anExpiredLinkIsRefused() throws Exception {
        registerUser.register(new RegisterUserCommand(DEMO, new Email("test-reset-meena@orudesk.org"), "old-password"));
        requestReset("test-reset-meena@orudesk.org").andExpect(status().isAccepted());
        String token = tokenFromEmail("test-reset-meena@orudesk.org");
        jdbc.update("""
                UPDATE iam_password_reset SET created_at = now() - interval '2 hours', expires_at = now() - interval '1 hour'
                WHERE user_id = (SELECT id FROM iam_user WHERE email = 'test-reset-meena@orudesk.org')
                """);

        confirm(token, "new-password").andExpect(status().isBadRequest());
        login("test-reset-meena@orudesk.org", "old-password").andExpect(status().isOk());
    }

    @Test
    void aShortNewPasswordIsAValidationError() throws Exception {
        confirm("some-token", "short").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"));
    }

    private String tokenFromEmail(String address) {
        await().atMost(Duration.ofSeconds(10)).until(() -> resetEmails(address).size() == 1);
        EmailMessage email = resetEmails(address).getFirst();
        Matcher link = LINK.matcher(email.text());
        assertThat(link.find()).as("a reset link in: %s", email.text()).isTrue();
        return link.group(1);
    }

    private List<EmailMessage> resetEmails(String address) {
        return mail.sentTo(address).stream().filter(m -> m.subject().equals("Reset your Orudesk password")).toList();
    }

    private ResultActions requestReset(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/password-reset").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + email + "\"}"));
    }

    private ResultActions confirm(String token, String newPassword) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"" + token + "\", \"newPassword\": \"" + newPassword + "\"}"));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}"));
    }
}
