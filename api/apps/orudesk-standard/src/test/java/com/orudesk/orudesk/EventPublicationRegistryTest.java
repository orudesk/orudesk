package com.orudesk.orudesk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * The outbox (Day 51): an event for an @ApplicationModuleListener is stored with the publisher's transaction,
 * the listener runs after the commit, and a listener that fails leaves the publication behind to be resubmitted.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, EventPublicationRegistryTest.Probe.class})
class EventPublicationRegistryTest {

    /** Stands in for "send the welcome email": something that must happen after the commit, at least once. */
    record ProbeEvent(String id) {
    }

    /** Fields behind methods: the listener is @Transactional, so tests talk to a proxy, which has no state. */
    @TestConfiguration(proxyBeanMethods = false)
    static class Probe {

        private final Set<String> handled = ConcurrentHashMap.newKeySet();
        private final Set<String> failNextTime = ConcurrentHashMap.newKeySet();

        @ApplicationModuleListener
        void on(ProbeEvent event) {
            if (failNextTime.remove(event.id())) {
                throw new IllegalStateException("the mail server is down");
            }
            handled.add(event.id());
        }

        boolean handled(String id) {
            return handled.contains(id);
        }

        void failNextTime(String id) {
            failNextTime.add(id);
        }
    }

    @Autowired
    private ApplicationEventPublisher events;

    @Autowired
    private TransactionTemplate transaction;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private Probe probe;

    @Autowired
    private IncompleteEventPublications incomplete;

    @Test
    void anEventIsStoredWithItsTransactionAndCompletedWhenTheListenerSucceeds() {
        String id = UUID.randomUUID().toString();

        transaction.executeWithoutResult(status -> events.publishEvent(new ProbeEvent(id)));

        await().atMost(Duration.ofSeconds(10)).until(() -> probe.handled(id));
        await().atMost(Duration.ofSeconds(10)).until(() -> publication(id).get("completion_date") != null);
        assertThat(publication(id)).containsEntry("status", "COMPLETED");
    }

    @Test
    void nothingIsStoredOrDoneWhenThePublisherRollsBack() {
        String id = UUID.randomUUID().toString();

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            events.publishEvent(new ProbeEvent(id));
            throw new IllegalStateException("the change failed");
        })).hasMessage("the change failed");

        assertThat(publications(id)).isEmpty();
        assertThat(probe.handled(id)).isFalse();   // no email about a change that never happened
    }

    @Test
    void aFailedListenerLeavesThePublicationToBeResubmitted() {
        String id = UUID.randomUUID().toString();
        probe.failNextTime(id);

        transaction.executeWithoutResult(status -> events.publishEvent(new ProbeEvent(id)));

        await().atMost(Duration.ofSeconds(10)).until(() -> "FAILED".equals(publication(id).get("status")));
        assertThat(publication(id).get("completion_date")).isNull();
        assertThat(probe.handled(id)).isFalse();

        // What a restart, an operator or Day 54's scheduled retry does: hand the stored event to the listener again
        incomplete.resubmitIncompletePublications(publication -> publication.getEvent() instanceof ProbeEvent e
                && e.id().equals(id));

        await().atMost(Duration.ofSeconds(10)).until(() -> probe.handled(id));
        await().atMost(Duration.ofSeconds(10)).until(() -> publication(id).get("completion_date") != null);
    }

    private Map<String, Object> publication(String id) {
        List<Map<String, Object>> rows = publications(id);
        assertThat(rows).hasSize(1);
        return rows.getFirst();
    }

    private List<Map<String, Object>> publications(String id) {
        return jdbc.queryForList("""
                SELECT status, completion_date, completion_attempts FROM event_publication
                WHERE event_type LIKE '%ProbeEvent' AND serialized_event LIKE ?
                """, "%" + id + "%");
    }
}
