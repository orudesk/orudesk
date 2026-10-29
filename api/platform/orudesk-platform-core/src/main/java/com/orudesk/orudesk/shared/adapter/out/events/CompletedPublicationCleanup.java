package com.orudesk.orudesk.shared.adapter.out.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Completed publications are kept for a while (a week by default), so "did the welcome email go out?" can be
 * answered from the table, then deleted: the table holds work, not history. The audit log is the history.
 */
@Component
class CompletedPublicationCleanup {

    private static final Logger log = LoggerFactory.getLogger(CompletedPublicationCleanup.class);

    private final CompletedEventPublications completed;
    private final Duration keep;

    CompletedPublicationCleanup(CompletedEventPublications completed,
                                @Value("${orudesk.events.keep-completed:7d}") Duration keep) {
        this.completed = completed;
        this.keep = keep;
    }

    /** Once an hour. With several app instances each one runs it; deleting the same old rows twice is harmless. */
    @Scheduled(fixedDelayString = "${orudesk.events.cleanup-interval:1h}", initialDelayString = "5m")
    void deleteOldCompletedPublications() {
        completed.deletePublicationsOlderThan(keep);
        log.debug("Deleted event publications completed more than {} ago", keep);
    }
}
