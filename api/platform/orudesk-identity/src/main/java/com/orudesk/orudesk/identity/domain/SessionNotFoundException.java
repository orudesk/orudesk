package com.orudesk.orudesk.identity.domain;

import java.util.UUID;

/** No such session for this user: never theirs, or already ended. Both look the same, on purpose. */
public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(UUID sessionId) {
        super("no session " + sessionId);
    }
}
