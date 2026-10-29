package com.orudesk.orudesk.shared.adapter.out.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.modulith.events.EventPublication;
import org.springframework.modulith.events.FailedEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Tries failed publications again (Day 54), with exponential backoff: 1, 2, 4, 8... minutes after the last
 * attempt, at most an hour apart, and gives up after {@code max-attempts}. A given-up publication stays FAILED in
 * the table, counted by the orudesk.events.failed gauge, for a person to look at.
 *
 * Why not just retry everything every minute? A mail server that's down for an hour would get sixty retries of
 * every email; one that refuses a message for good (a bad address) would get them forever.
 */
@Component
class FailedPublicationRetry {

    private static final Logger log = LoggerFactory.getLogger(FailedPublicationRetry.class);
    private static final Duration MAX_BACKOFF = Duration.ofHours(1);

    private final FailedEventPublications failed;
    private final Clock clock;
    private final Duration firstBackoff;
    private final int maxAttempts;

    FailedPublicationRetry(FailedEventPublications failed, Clock clock,
                           @Value("${orudesk.events.retry.first-backoff:1m}") Duration firstBackoff,
                           @Value("${orudesk.events.retry.max-attempts:10}") int maxAttempts) {
        this.failed = failed;
        this.clock = clock;
        this.firstBackoff = firstBackoff;
        this.maxAttempts = maxAttempts;
    }

    /**
     * Every instance runs this. Two instances can't both resubmit one publication: Modulith marks it RESUBMITTED
     * with "UPDATE ... WHERE status != 'RESUBMITTED'", and only the instance whose update hit a row runs it.
     */
    @Scheduled(fixedDelayString = "${orudesk.events.retry.interval:30s}",
            initialDelayString = "${orudesk.events.retry.initial-delay:1m}")
    void resubmitFailedPublications() {
        failed.resubmit(ResubmissionOptions.defaults()
                .withBatchSize(100)
                .withMaxInFlight(10)   // a backlog of a thousand emails goes out ten at a time, not all at once
                .withFilter(this::dueForRetry));
    }

    boolean dueForRetry(EventPublication publication) {
        int attempts = publication.getCompletionAttempts();
        if (attempts >= maxAttempts) {
            log.debug("Not retrying {} any more, after {} attempts", publication.getIdentifier(), attempts);
            return false;
        }
        Instant last = publication.getLastResubmissionDate() != null
                ? publication.getLastResubmissionDate()
                : publication.getPublicationDate();
        return !clock.instant().isBefore(last.plus(backoff(attempts)));
    }

    /** firstBackoff * 2^(attempts - 1), capped. attempts is 1 after the first, failed delivery. */
    Duration backoff(int attempts) {
        int doublings = Math.min(Math.max(attempts - 1, 0), 20);   // 2^20 minutes is long past the cap anyway
        Duration backoff = firstBackoff.multipliedBy(1L << doublings);
        return backoff.compareTo(MAX_BACKOFF) > 0 ? MAX_BACKOFF : backoff;
    }
}
