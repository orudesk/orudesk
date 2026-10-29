package com.orudesk.orudesk.identity.application.port.out;

import com.orudesk.orudesk.identity.domain.UserId;

import java.util.Optional;

public interface RefreshTokenStore {

    /** Creates a new random refresh token for this user and remembers it. */
    String create(UserId userId);

    /** Returns the token's user and forgets the token, so it can be used only once. */
    Optional<UserId> consume(String refreshToken);

    void revoke(String refreshToken);

    /** Ends every session of this user: none of their refresh tokens works any more (Day 53). */
    void revokeAll(UserId userId);
}
