package com.orudesk.orudesk.identity.application.port.out;

import com.orudesk.orudesk.identity.domain.UserId;

import java.util.UUID;

/** Whose session a spent refresh token belonged to (Day 66). */
public record SessionRef(UserId userId, UUID sessionId) {
}
