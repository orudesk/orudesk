package com.orudesk.orudesk.identity.application.port.in;

import com.orudesk.orudesk.identity.domain.Email;

import java.util.Objects;

public record LoginCommand(Email email, String rawPassword) {

    public LoginCommand {
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(rawPassword, "password must not be null");
    }

    @Override
    public String toString() {
        return "LoginCommand[email=" + email + ", rawPassword=****]";
    }
}
