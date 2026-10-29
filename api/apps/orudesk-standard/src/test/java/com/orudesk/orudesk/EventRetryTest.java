package com.orudesk.orudesk;

import com.orudesk.orudesk.identity.application.port.in.RegisterUserCommand;
import com.orudesk.orudesk.identity.application.port.in.RegisterUserUseCase;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.TenantId;
import com.orudesk.orudesk.shared.ProcessedEvents;
import com.orudesk.orudesk.shared.RecordingEmailSender;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** Day 54: failed work is retried by itself, done once, and counted where monitoring can see it. */
@SpringBootTest(properties = {
        "orudesk.events.retry.interval=500ms",
        "orudesk.events.retry.initial-delay=500ms",
        "orudesk.events.retry.first-backoff=1s"})
@Import({TestcontainersConfiguration.class, EventRetryTest.Mail.class})
class EventRetryTest {

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
    private ProcessedEvents processed;

    @Autowired
    private TransactionTemplate transactions;

    @Autowired
    private MeterRegistry meters;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void cleanUp() {
        mail.setDown(false);
        jdbc.update("DELETE FROM iam_user WHERE email LIKE 'test-retry-%@orudesk.org'");
        jdbc.update("DELETE FROM event_receipt WHERE receipt_key LIKE 'test-retry-%'");
    }

    @Test
    void anEmailThatFailedIsSentOnceTheMailServerIsBack() {
        mail.setDown(true);
        registerUser.register(new RegisterUserCommand(DEMO, new Email("test-retry-asha@orudesk.org"), "asha-password"));
        await().atMost(Duration.ofSeconds(10)).until(() -> failedWelcomes("test-retry-asha@orudesk.org") == 1);
        await().atMost(Duration.ofSeconds(10)).until(() -> meters.get("orudesk.events.failed").gauge().value() >= 1);

        mail.setDown(false);

        await().atMost(Duration.ofSeconds(20)).until(() -> !mail.sentTo("test-retry-asha@orudesk.org").isEmpty());
        await().atMost(Duration.ofSeconds(10)).until(() -> failedWelcomes("test-retry-asha@orudesk.org") == 0);
        assertThat(mail.sentTo("test-retry-asha@orudesk.org")).hasSize(1);
        assertThat(jdbc.queryForObject("""
                SELECT completion_attempts FROM event_publication
                WHERE listener_id LIKE '%WelcomeEmailListener%' AND serialized_event LIKE '%test-retry-asha@orudesk.org%'
                """, Integer.class)).isGreaterThan(1);
    }

    @Test
    void aReceiptKeyIsFirstOnlyOnceAndARolledBackOneDoesNotCount() {
        Boolean rolledBack = transactions.execute(status -> {
            boolean first = processed.firstTime("test-retry-key");
            status.setRollbackOnly();   // the work after it failed
            return first;
        });
        assertThat(rolledBack).isTrue();

        Boolean again = transactions.execute(status -> processed.firstTime("test-retry-key"));
        Boolean third = transactions.execute(status -> processed.firstTime("test-retry-key"));
        assertThat(again).isTrue();
        assertThat(third).isFalse();
    }

    private int failedWelcomes(String email) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM event_publication
                WHERE listener_id LIKE '%WelcomeEmailListener%' AND status = 'FAILED' AND serialized_event LIKE ?
                """, Integer.class, "%" + email + "%");
    }
}
