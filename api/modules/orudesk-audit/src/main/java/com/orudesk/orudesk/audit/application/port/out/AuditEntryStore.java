package com.orudesk.orudesk.audit.application.port.out;

import com.orudesk.orudesk.audit.domain.AuditEntry;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface AuditEntryStore {

    void append(AuditEntry entry);

    /** The current tenant's entries before {@code before} (or the newest), newest first, at most {@code limit}. */
    List<AuditEntry> findNewest(@Nullable UUID subjectId, @Nullable AuditPosition before, int limit);
}
