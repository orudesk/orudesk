package com.orudesk.orudesk.identity.application.port.out;

import com.orudesk.orudesk.identity.application.port.in.ClientInfo;
import com.orudesk.orudesk.identity.domain.UserId;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Refresh tokens, grouped into sessions (Day 66). A session starts at login and lives through every refresh: each
 * refresh spends the old token and gives the same session a new one. Ending a session (logout, "log out
 * everywhere", a password reset) deletes it, and the access tokens that name it stop working too.
 */
public interface RefreshTokenStore {

    /** A new session for this user, with its first refresh token. */
    RefreshGrant start(UserId userId, ClientInfo client);

    /** Returns the token's session and forgets the token, so it can be used only once. The session goes on. */
    Optional<SessionRef> consume(String refreshToken);

    /** The next refresh token of a session whose token was just consumed. */
    String renew(SessionRef session, ClientInfo client);

    /** Logout: forgets the token and ends its session. */
    void revoke(String refreshToken);

    /** The user's sessions, most recently used first. */
    List<SessionDetails> sessions(UserId userId);

    /** Ends one of the user's sessions; false when they have no such session. */
    boolean end(UserId userId, UUID sessionId);

    /** Ends every session of the user except one (null: all of them); returns how many ended. */
    int endAllExcept(UserId userId, @Nullable UUID keep);

    /** Ends every session of this user: none of their refresh tokens works any more (Day 53). */
    default void revokeAll(UserId userId) {
        endAllExcept(userId, null);
    }

    /** Is this session still going? Asked for every access token that names one. */
    boolean isActive(UUID sessionId);
}
