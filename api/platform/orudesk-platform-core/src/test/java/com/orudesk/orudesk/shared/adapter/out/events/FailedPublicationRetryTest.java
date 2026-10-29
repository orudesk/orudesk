package com.orudesk.orudesk.shared.adapter.out.events;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.events.EventPublication;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FailedPublicationRetryTest {

    private static final Instant NOW = Instant.parse("2026-10-29T12:00:00Z");

    private final FailedPublicationRetry retry =
            new FailedPublicationRetry(options -> { }, Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofMinutes(1), 10);

    @Test
    void theWaitDoublesAfterEachAttemptUpToAnHour() {
        assertThat(retry.backoff(1)).isEqualTo(Duration.ofMinutes(1));
        assertThat(retry.backoff(2)).isEqualTo(Duration.ofMinutes(2));
        assertThat(retry.backoff(4)).isEqualTo(Duration.ofMinutes(8));
        assertThat(retry.backoff(7)).isEqualTo(Duration.ofHours(1));   // 64 minutes, capped
        assertThat(retry.backoff(1000)).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void aPublicationIsDueWhenItsBackoffHasPassedSinceTheLastAttempt() {
        assertThat(retry.dueForRetry(publication(1, NOW.minusSeconds(61), null))).isTrue();
        assertThat(retry.dueForRetry(publication(1, NOW.minusSeconds(30), null))).isFalse();
        // After the third attempt the wait is 4 minutes, counted from that resubmission, not the first publication
        assertThat(retry.dueForRetry(publication(3, NOW.minus(Duration.ofDays(1)), NOW.minusSeconds(200)))).isFalse();
        assertThat(retry.dueForRetry(publication(3, NOW.minus(Duration.ofDays(1)), NOW.minusSeconds(250)))).isTrue();
    }

    @Test
    void afterTheLastAttemptItIsLeftForAPerson() {
        assertThat(retry.dueForRetry(publication(10, NOW.minus(Duration.ofDays(1)), NOW.minus(Duration.ofDays(1)))))
                .isFalse();
    }

    private static EventPublication publication(int attempts, Instant published, Instant lastResubmission) {
        return new EventPublication() {
            @Override public UUID getIdentifier() { return UUID.randomUUID(); }
            @Override public Object getEvent() { return "event"; }
            @Override public Instant getPublicationDate() { return published; }
            @Override public Optional<Instant> getCompletionDate() { return Optional.empty(); }
            @Override public Status getStatus() { return Status.FAILED; }
            @Override public Instant getLastResubmissionDate() { return lastResubmission; }
            @Override public int getCompletionAttempts() { return attempts; }
        };
    }
}
