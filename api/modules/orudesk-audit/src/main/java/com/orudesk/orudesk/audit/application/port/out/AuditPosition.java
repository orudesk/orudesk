package com.orudesk.orudesk.audit.application.port.out;

import java.time.Instant;
import java.util.UUID;

/** Where a page ends: entries are sorted by time, then id, both newest first. */
public record AuditPosition(Instant occurredAt, UUID id) {
}
