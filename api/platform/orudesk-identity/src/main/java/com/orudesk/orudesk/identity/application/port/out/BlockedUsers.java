package com.orudesk.orudesk.identity.application.port.out;

import com.orudesk.orudesk.identity.domain.UserId;

/**
 * The users whose access tokens must be refused before they expire (Day 48). An access token is checked by its
 * signature alone, so a disabled user's token would keep working for up to its lifetime (15 minutes); this list
 * closes that gap. An entry only needs to live as long as a token can: after that, every token issued before the
 * user was disabled has expired anyway, and they can't get a new one.
 */
public interface BlockedUsers {

    void block(UserId userId);

    void unblock(UserId userId);

    boolean isBlocked(UserId userId);
}
