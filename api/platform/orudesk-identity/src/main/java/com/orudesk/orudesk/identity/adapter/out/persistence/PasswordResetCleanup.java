package com.orudesk.orudesk.identity.adapter.out.persistence;

import com.orudesk.orudesk.identity.application.port.out.PasswordResetTokens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

/**
 * Deletes reset tokens a day after they expired (Day 53). Used or not, an expired token can't do anything; the
 * extra day keeps the row for "my link said it was invalid" questions to support.
 */
@Component
class PasswordResetCleanup {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetCleanup.class);

    private final PasswordResetTokens tokens;
    private final Clock clock;

    PasswordResetCleanup(PasswordResetTokens tokens, Clock clock) {
        this.tokens = tokens;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "6h", initialDelayString = "10m")
    void deleteExpired() {
        int deleted = tokens.deleteExpiredBefore(clock.instant().minus(Duration.ofDays(1)));
        if (deleted > 0) {
            log.info("Deleted {} expired password reset tokens", deleted);
        }
    }
}
