package com.orudesk.orudesk.identity.application.port.in;

import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.UserId;

import java.util.Optional;

/**
 * "I forgot my password" (Day 53), in three steps:
 * <ol>
 *   <li>{@link #requestReset}: someone types an email. Nothing is said about whether it has an account.</li>
 *   <li>{@link #issueToken}: after that commits, the email listener asks for a one-time token and mails a link.</li>
 *   <li>{@link #resetPassword}: the link's page sends the token and a new password.</li>
 * </ol>
 */
public interface PasswordResetUseCase {

    /** Publishes PasswordResetRequested if the email belongs to an enabled user; otherwise does nothing, silently. */
    void requestReset(Email email);

    /** A new one-time token for this user, or empty if they can't log in any more (disabled since the request). */
    Optional<String> issueToken(UserId userId);

    /**
     * Sets the new password, ends every session of the user, and lifts a login block. Throws
     * InvalidPasswordResetTokenException if the token is unknown, expired or used.
     */
    void resetPassword(String token, String newPassword);
}
