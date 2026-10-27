package com.orudesk.orudesk.audit.application.service;

import com.orudesk.orudesk.audit.application.port.out.AuditPosition;
import com.orudesk.orudesk.audit.domain.AuditEntry;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/** The last entry of a page as an opaque string for clients (the same idea as PersonCursor). */
final class AuditCursor {

    private AuditCursor() {
    }

    static String encode(AuditEntry last) {
        String raw = last.occurredAt() + " " + last.id();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    static AuditPosition decode(String cursor) {
        try {
            String[] parts = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split(" ");
            if (parts.length != 2) {
                throw new IllegalArgumentException("invalid cursor");
            }
            return new AuditPosition(Instant.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (IllegalArgumentException | java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("invalid cursor", e);
        }
    }
}
