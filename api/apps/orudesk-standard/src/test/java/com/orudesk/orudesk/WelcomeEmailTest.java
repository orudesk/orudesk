package com.orudesk.orudesk;

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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** A new user gets a welcome email (Day 52), after the registration commits, through the EmailSender port. */
@SpringBootTest(properties = "orudesk.web.base-url=https://erp.example.org")
@Import({TestcontainersConfiguration.class, WelcomeEmailTest.Mail.class})
class WelcomeEmailTest {

    private static final TenantId DEMO = new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    @TestConfiguration(proxyBeanMethods = false)
    static class Mail {
        @Bean
        @Primary
        RecordingEmailSender recordingEmailSender() {
            return new RecordingEmailSender();
        }
    }

    @Autowired
    private RegisterUserUseCase registerUser;

    @Autowired
    private RecordingEmailSender mail;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void cleanUp() {
        mail.setDown(false);
        jdbc.update("DELETE FROM iam_user WHERE email LIKE 'test-welcome-%@orudesk.org'");
    }

    @Test
    void aNewUserIsWelcomedWithALinkToTheWebApp() {
        registerUser.register(new RegisterUserCommand(DEMO, new Email("test-welcome-asha@orudesk.org"), "asha-password"));

        await().atMost(Duration.ofSeconds(10)).until(() -> !mail.sentTo("test-welcome-asha@orudesk.org").isEmpty());
        EmailMessage welcome = mail.sentTo("test-welcome-asha@orudesk.org").getFirst();
        assertThat(welcome.subject()).isEqualTo("Welcome to Orudesk");
        assertThat(welcome.text()).contains("https://erp.example.org/login").doesNotContain("asha-password");
    }

    @Test
    void aMailServerThatIsDownDoesNotFailTheRegistration() {
        mail.setDown(true);

        registerUser.register(new RegisterUserCommand(DEMO, new Email("test-welcome-ravi@orudesk.org"), "ravi-password"));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM iam_user WHERE email = 'test-welcome-ravi@orudesk.org'",
                Integer.class)).isEqualTo(1);
        // ...and the email is still owed: its publication waits, failed, for a retry (Day 54)
        await().atMost(Duration.ofSeconds(10)).until(() -> jdbc.queryForObject("""
                SELECT COUNT(*) FROM event_publication
                WHERE listener_id LIKE '%WelcomeEmailListener%' AND status = 'FAILED'
                  AND serialized_event LIKE '%test-welcome-ravi@orudesk.org%'
                """, Integer.class) == 1);
    }
}
