package com.orudesk.orudesk.identity.application.port.out;

import com.orudesk.orudesk.identity.domain.User;

import java.util.UUID;

public interface AccessTokenIssuer {

    /** An access token for this user, naming the session it belongs to (Day 66). */
    AccessToken issue(User user, UUID sessionId);
}
