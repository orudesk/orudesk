package com.orudesk.orudesk.identity.application.port.in;

import java.util.Objects;

/**
 * The login's second step (Day 68).
 *
 * @param loginToken from the first step's "Second factor required" answer
 * @param code       six digits from the authenticator app, or one of the recovery codes
 */
public record SecondFactorCommand(String loginToken, String code, ClientInfo client) {

    public SecondFactorCommand {
        Objects.requireNonNull(loginToken, "loginToken must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(client, "client must not be null");
    }

    @Override
    public String toString() {
        return "SecondFactorCommand[loginToken=****, code=****, client=" + client + "]";
    }
}
