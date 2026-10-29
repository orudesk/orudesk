package com.orudesk.orudesk.identity.domain;

/** No encryption key configured for the secrets (Day 67). */
public class TwoFactorNotAvailableException extends RuntimeException {

    public TwoFactorNotAvailableException() {
        super("two-factor sign-in is not available on this server: ORUDESK_MFA_KEY is not set");
    }
}
