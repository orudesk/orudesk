package com.orudesk.orudesk.audit.application.service;

import com.orudesk.orudesk.audit.application.port.in.Actor;
import com.orudesk.orudesk.audit.application.port.in.AuditEntryDetails;
import com.orudesk.orudesk.audit.application.port.in.AuditPage;
import com.orudesk.orudesk.audit.application.port.in.AuditSearch;
import com.orudesk.orudesk.audit.application.port.in.AuditTrailQuery;
import com.orudesk.orudesk.audit.application.port.in.RecordAuditEntryUseCase;
import com.orudesk.orudesk.audit.application.port.out.AuditEntryStore;
import com.orudesk.orudesk.audit.application.port.out.AuditPosition;
import com.orudesk.orudesk.audit.domain.AuditEntry;
import com.orudesk.orudesk.shared.AuditableEvent;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AuditService implements RecordAuditEntryUseCase, AuditTrailQuery {

    private final AuditEntryStore store;
    private final Clock clock;

    public AuditService(AuditEntryStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    @Override
    public void record(AuditableEvent event, UUID tenantId, Actor actor, @Nullable String requestId) {
        store.append(new AuditEntry(UUID.randomUUID(), tenantId,
                // PostgreSQL keeps microseconds; cutting here means the cursor matches what's stored
                clock.instant().truncatedTo(ChronoUnit.MICROS),
                actor.id(), actor.email(), event.action(), event.subjectType(), event.subjectId(),
                event.summary(), requestId, event.changes()));
    }

    @Override
    @Transactional(readOnly = true)
    public AuditPage list(AuditSearch search) {
        AuditPosition before = search.after() == null ? null : AuditCursor.decode(search.after());
        List<AuditEntry> found = store.findNewest(search.subjectId(), before, search.size() + 1);
        boolean hasMore = found.size() > search.size();
        List<AuditEntry> page = hasMore ? found.subList(0, search.size()) : found;
        return new AuditPage(page.stream().map(AuditService::toDetails).toList(),
                hasMore ? AuditCursor.encode(page.getLast()) : null);
    }

    private static AuditEntryDetails toDetails(AuditEntry entry) {
        return new AuditEntryDetails(entry.id(), entry.occurredAt(), entry.actorId(), entry.actorEmail(),
                entry.action(), entry.subjectType(), entry.subjectId(), entry.summary(), entry.requestId(),
                entry.changes());
    }
}
