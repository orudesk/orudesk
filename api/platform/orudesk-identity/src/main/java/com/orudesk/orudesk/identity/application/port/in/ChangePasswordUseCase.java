package com.orudesk.orudesk.identity.application.port.in;

import com.orudesk.orudesk.identity.domain.UserId;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/** A logged-in user changes their own password (Day 69). */
public interface ChangePasswordUseCase {

    /**
     * Checks the current password, sets the new one, and signs out every other session.
     *
     * @param currentSession the session making the change, which stays signed in; null signs out all of them
     */
    void changePassword(UserId userId, @Nullable UUID currentSession, String currentPassword, String newPassword);
}
