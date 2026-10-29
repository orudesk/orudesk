package com.orudesk.orudesk.identity.application.port.in;

import com.orudesk.orudesk.identity.domain.UserId;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/** A user's own sessions (Day 66): where they're signed in, and signing out of the ones they don't recognise. */
public interface SessionsUseCase {

    /** @param current the session of the asking request, marked in the list (null for an older token without one) */
    List<ActiveSession> list(UserId userId, @Nullable UUID current);

    /** @throws com.orudesk.orudesk.identity.domain.SessionNotFoundException if the user has no such session */
    void end(UserId userId, UUID sessionId);

    /** "Log out everywhere else": ends every session but the current one; returns how many ended. */
    int endOthers(UserId userId, @Nullable UUID current);
}
