package com.orudesk.orudesk.identity.application.port.out;

import java.time.Instant;
import java.util.UUID;

/** One signed-in device, as the store remembers it (Day 66). */
public record SessionDetails(UUID id, Instant startedAt, Instant lastUsedAt, String device, String ipAddress) {
}
