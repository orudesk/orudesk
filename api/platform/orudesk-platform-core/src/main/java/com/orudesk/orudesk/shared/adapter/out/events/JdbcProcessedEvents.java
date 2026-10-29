package com.orudesk.orudesk.shared.adapter.out.events;

import com.orudesk.orudesk.shared.ProcessedEvents;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;

/**
 * INSERT ... ON CONFLICT DO NOTHING: one statement, no "check then insert" race. PostgreSQL reports one row
 * inserted the first time and zero after; a concurrent insert of the same key waits on the first transaction.
 */
@Component
class JdbcProcessedEvents implements ProcessedEvents {

    private static final Duration KEEP = Duration.ofDays(30);

    private final JdbcTemplate jdbc;
    private final Clock clock;

    JdbcProcessedEvents(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)   // without the caller's transaction it couldn't be undone
    public boolean firstTime(String key) {
        if (key.isBlank() || key.length() > 200) {
            throw new IllegalArgumentException("a receipt key has 1 to 200 characters");
        }
        return jdbc.update("INSERT INTO event_receipt (receipt_key, received_at) VALUES (?, ?) ON CONFLICT DO NOTHING",
                key, Timestamp.from(clock.instant())) == 1;
    }

    /**
     * A month is far longer than any retry runs (Day 54's retries stop after a few hours), so a receipt older than
     * that protects against nothing.
     */
    @Scheduled(fixedDelayString = "${orudesk.events.cleanup-interval:1h}", initialDelayString = "7m")
    void deleteOldReceipts() {
        jdbc.update("DELETE FROM event_receipt WHERE received_at < ?", Timestamp.from(clock.instant().minus(KEEP)));
    }
}
