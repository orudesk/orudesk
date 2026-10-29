package com.orudesk.orudesk.identity.application.port.in;

import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.TenantId;

import java.util.Objects;

/**
 * Everything needed to register a user. Checks the password policy before any work starts.
 *
 * @param checkBreaches whether to refuse a password known from breaches (Day 69): yes for a password a person
 *                      types, no for one an operator put in the configuration (the first admin, the demo's users),
 *                      where refusing would stop the application from starting
 */
public record RegisterUserCommand(TenantId tenantId, Email email, String rawPassword, boolean checkBreaches) {

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

    public RegisterUserCommand(TenantId tenantId, Email email, String rawPassword) {
        this(tenantId, email, rawPassword, true);
    }

    /** A password from the configuration, not typed by the user: not checked against breaches. */
    public static RegisterUserCommand fromConfiguration(TenantId tenantId, Email email, String rawPassword) {
        return new RegisterUserCommand(tenantId, email, rawPassword, false);
    }

    @Override
    public String toString() {
        return "RegisterUserCommand[tenantId=" + tenantId + ", email=" + email + ", rawPassword=****, checkBreaches=" + checkBreaches + "]";
    }
}
