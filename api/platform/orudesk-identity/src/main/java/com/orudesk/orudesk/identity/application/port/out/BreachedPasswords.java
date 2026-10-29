package com.orudesk.orudesk.identity.application.port.out;

/**
 * Whether a password is known from a data breach (Day 69). Attackers try those first, in every login form, so a
 * leaked password is a weak one however long it is (NIST SP 800-63B, 5.1.1.2).
 */
public interface BreachedPasswords {

    /** False when unsure (the service is down): the check makes passwords better, it must not stop sign-ups. */
    boolean isBreached(String rawPassword);
}
