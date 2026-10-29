package com.orudesk.orudesk.audit.application.port.in;

import com.orudesk.orudesk.shared.FieldChange;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuditEntryDetails(UUID id, Instant occurredAt, @Nullable UUID actorId, @Nullable String actorEmail,
                                String action, String subjectType, UUID subjectId, String summary,
                                @Nullable String requestId, List<FieldChange> changes) {
}
