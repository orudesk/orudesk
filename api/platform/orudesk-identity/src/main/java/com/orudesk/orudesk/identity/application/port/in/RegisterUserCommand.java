package com.orudesk.orudesk.identity.application.port.in;

import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.TenantId;

import java.util.Objects;

/** Everything needed to register a user. Checks the password policy before any work starts. */
public record RegisterUserCommand(TenantId tenantId, Email email, String rawPassword) {

    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 72;

    public RegisterUserCommand {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(rawPassword, "password must not be null");
        if (rawPassword.length() < MIN_PASSWORD_LENGTH || rawPassword.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("password must be " + MIN_PASSWORD_LENGTH
                    + " to " + MAX_PASSWORD_LENGTH + " characters");
        }
    }

    @Override
    public String toString() {
        return "RegisterUserCommand[tenantId=" + tenantId + ", email=" + email + ", rawPassword=****]";
    }
}
