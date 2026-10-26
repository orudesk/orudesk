package com.orudesk.orudesk.audit.application.port.in;

import com.orudesk.orudesk.shared.AuditableEvent;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public interface RecordAuditEntryUseCase {

    /** Records the event in the caller's transaction: if the change rolls back, so does its entry. */
    void record(AuditableEvent event, UUID tenantId, Actor actor, @Nullable String requestId);
}
