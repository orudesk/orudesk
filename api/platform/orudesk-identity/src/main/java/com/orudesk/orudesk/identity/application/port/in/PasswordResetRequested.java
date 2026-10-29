package com.orudesk.orudesk.identity.application.port.in;

import java.util.UUID;

/**
 * Someone asked for a password reset link for this user (Day 53). Deliberately carries no token: the event is
 * stored in the outbox (event_publication) as JSON, and a secret there would be readable by anyone who reads that
 * table. The listener creates the token at the moment it sends the email.
 */
public record PasswordResetRequested(UUID userId, String email) {
}
