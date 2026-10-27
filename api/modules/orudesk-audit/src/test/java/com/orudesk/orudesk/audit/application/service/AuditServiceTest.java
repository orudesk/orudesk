package com.orudesk.orudesk.audit.application.service;

import com.orudesk.orudesk.audit.application.port.in.Actor;
import com.orudesk.orudesk.audit.application.port.in.AuditEntryDetails;
import com.orudesk.orudesk.audit.application.port.in.AuditPage;
import com.orudesk.orudesk.audit.application.port.in.AuditSearch;
import com.orudesk.orudesk.audit.application.port.out.AuditEntryStore;
import com.orudesk.orudesk.audit.application.port.out.AuditPosition;
import com.orudesk.orudesk.audit.domain.AuditEntry;
import com.orudesk.orudesk.shared.AuditableEvent;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final Actor ANNA = new Actor(UUID.randomUUID(), "anna@orudesk.org");

    /** A list sorted the way the database sorts: newest first, then by id. */
    private final List<AuditEntry> rows = new ArrayList<>();
    private final AuditEntryStore store = new AuditEntryStore() {
        @Override
        public void append(AuditEntry entry) {
            rows.add(entry);
        }

        @Override
        public List<AuditEntry> findNewest(@Nullable UUID subjectId, @Nullable AuditPosition before, int limit) {
            Comparator<AuditEntry> newestFirst = Comparator.comparing(AuditEntry::occurredAt)
                    .thenComparing(AuditEntry::id).reversed();
            return rows.stream()
                    .filter(e -> subjectId == null || e.subjectId().equals(subjectId))
                    .filter(e -> before == null || e.occurredAt().isBefore(before.occurredAt())
                            || e.occurredAt().equals(before.occurredAt()) && e.id().compareTo(before.id()) < 0)
                    .sorted(newestFirst).limit(limit).toList();
        }
    };

    private Instant now = Instant.parse("2026-10-26T10:00:00.123456789Z");
    private final AuditService service = new AuditService(store, new Clock() {
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    });

    private record Happened(UUID subjectId, String summary) implements AuditableEvent {
        @Override public String action() { return "test.thing.happened"; }
        @Override public String subjectType() { return "thing"; }
    }

    @Test
    void recordsWhoDidWhatAndInWhichRequest() {
        UUID thing = UUID.randomUUID();

        service.record(new Happened(thing, "Did a thing"), TENANT, ANNA, "req-12345678");

        AuditEntry entry = rows.getFirst();
        assertThat(entry.actorEmail()).isEqualTo("anna@orudesk.org");
        assertThat(entry.action()).isEqualTo("test.thing.happened");
        assertThat(entry.subjectId()).isEqualTo(thing);
        assertThat(entry.requestId()).isEqualTo("req-12345678");
        // PostgreSQL keeps microseconds, so we do too: a cursor from a stored row must match it exactly
        assertThat(entry.occurredAt()).isEqualTo(Instant.parse("2026-10-26T10:00:00.123456Z"));
    }

    @Test
    void pagesNewestFirstWithACursor() {
        for (int i = 1; i <= 5; i++) {
            service.record(new Happened(UUID.randomUUID(), "Thing " + i), TENANT, ANNA, null);
            now = now.plusSeconds(1);
        }

        AuditPage first = service.list(new AuditSearch(null, null, 2));
        AuditPage second = service.list(new AuditSearch(null, first.nextCursor(), 2));
        AuditPage last = service.list(new AuditSearch(null, second.nextCursor(), 2));

        assertThat(first.items()).extracting(AuditEntryDetails::summary).containsExactly("Thing 5", "Thing 4");
        assertThat(second.items()).extracting(AuditEntryDetails::summary).containsExactly("Thing 3", "Thing 2");
        assertThat(last.items()).extracting(AuditEntryDetails::summary).containsExactly("Thing 1");
        assertThat(last.nextCursor()).isNull();
    }

    @Test
    void listsWhatHappenedToOneThing() {
        UUID asha = UUID.randomUUID();
        service.record(new Happened(asha, "Registered Asha"), TENANT, ANNA, null);
        service.record(new Happened(UUID.randomUUID(), "Something else"), TENANT, ANNA, null);

        assertThat(service.list(new AuditSearch(asha, null, 10)).items())
                .extracting(AuditEntryDetails::summary).containsExactly("Registered Asha");
    }

    @Test
    void aLongSummaryIsCutNotRefused() {
        service.record(new Happened(UUID.randomUUID(), "x".repeat(600)), TENANT, ANNA, null);

        assertThat(rows.getFirst().summary()).hasSize(500).endsWith("…");
    }

    @Test
    void aMangledCursorIsBadInput() {
        assertThatThrownBy(() -> service.list(new AuditSearch(null, "not-a-cursor", 10)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
