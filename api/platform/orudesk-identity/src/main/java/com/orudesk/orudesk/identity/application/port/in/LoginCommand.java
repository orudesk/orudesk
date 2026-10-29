package com.orudesk.orudesk.identity.application.port.in;

import com.orudesk.orudesk.identity.domain.Email;

import java.util.Objects;

/** @param client where the login comes from, for the user's list of sessions (Day 66) */
public record LoginCommand(Email email, String rawPassword, ClientInfo client) {

    public LoginCommand {
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(rawPassword, "password must not be null");
        Objects.requireNonNull(client, "client must not be null");
    }

    public LoginCommand(Email email, String rawPassword) {
        this(email, rawPassword, ClientInfo.unknown());
    }

    @Override
    public String toString() {
        return "LoginCommand[email=" + email + ", rawPassword=****, client=" + client + "]";
    }
}
