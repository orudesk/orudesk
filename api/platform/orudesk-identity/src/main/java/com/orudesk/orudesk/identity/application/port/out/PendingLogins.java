package com.orudesk.orudesk.identity.application.port.out;

import com.orudesk.orudesk.identity.domain.UserId;

import java.util.Optional;

/**
 * Logins halfway through (Day 68): the password was right and the second factor is still to come. Each is known by a
 * random token the client holds for a few minutes; it is not a login, and opens nothing but the second step.
 */
public interface PendingLogins {

    /** Wrong codes allowed per pending login; then it ends and the user starts again with the password. */
    int MAX_FAILURES = 5;

    /** @return the token for the second step */
    String start(UserId userId);

    Optional<UserId> find(String token);

    /** Counts a wrong code; the last allowed one ends the pending login. */
    void failed(String token);

    /** Ends it: true only for the first caller, so one pending login can't become two sessions. */
    boolean finish(String token);
}
