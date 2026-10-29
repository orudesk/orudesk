package com.orudesk.orudesk.identity.application.port.out;

import java.util.UUID;

/** A new refresh token, and the session it belongs to (Day 66): the session id goes into the access token. */
public record RefreshGrant(String token, UUID sessionId) {
}
