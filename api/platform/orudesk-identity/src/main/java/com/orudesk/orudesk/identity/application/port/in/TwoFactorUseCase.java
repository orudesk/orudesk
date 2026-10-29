package com.orudesk.orudesk.identity.application.port.in;

import com.orudesk.orudesk.identity.domain.UserId;

import java.util.List;

/** Two-factor sign-in with an authenticator app, set up by the user for themselves (Day 67). */
public interface TwoFactorUseCase {

    TwoFactorStatus status(UserId userId);

    /** Step 1: a new secret for the app (as text and as an otpauth:// link for a QR code). Asks for the password. */
    TotpSetup beginSetup(UserId userId, String currentPassword);

    /** Step 2: the first code from the app proves it's set up; two-factor sign-in is on. Returns the recovery codes. */
    List<String> confirmSetup(UserId userId, String code);

    /** New recovery codes; the old ones stop working. */
    List<String> newRecoveryCodes(UserId userId, String currentPassword);

    void disable(UserId userId, String currentPassword);
}
