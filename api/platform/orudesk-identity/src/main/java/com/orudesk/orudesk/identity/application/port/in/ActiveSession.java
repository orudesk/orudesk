package com.orudesk.orudesk.identity.application.port.in;

import java.time.Instant;
import java.util.UUID;

/** One place the user is signed in. {@code current} is the session the request itself belongs to. */
public record ActiveSession(UUID id, Instant startedAt, Instant lastUsedAt, String device, String ipAddress,
                            boolean current) {
}
