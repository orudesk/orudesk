package com.orudesk.orudesk.identity.application.port.out;

import com.orudesk.orudesk.identity.domain.UserId;

import java.time.Instant;
import java.util.Optional;

/**
 * One-time tokens that let a user set a new password (Day 53). The raw token only ever exists in the email;
 * the store keeps a hash.
 */
public interface PasswordResetTokens {

    /** A new token for this user, valid for a short while. Any older unused token of theirs stops working. */
    String create(UserId userId);

    /** The token's user, if the token exists, hasn't expired and wasn't used; it is used up by this call. */
    Optional<UserId> consume(String rawToken, Instant now);

    /** Forgets tokens that expired before {@code cutoff}. Returns how many. */
    int deleteExpiredBefore(Instant cutoff);
}
