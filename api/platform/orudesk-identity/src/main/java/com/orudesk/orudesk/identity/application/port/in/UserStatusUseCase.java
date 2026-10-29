package com.orudesk.orudesk.identity.application.port.in;

import com.orudesk.orudesk.identity.domain.UserId;

/**
 * Turns a login off and on again (Day 48), for someone who left, or an account that may be compromised. Users
 * are never deleted: the audit log and the records they made still point at them.
 */
public interface UserStatusUseCase {

    /**
     * Stops the user from logging in and ends their sessions within seconds. Disabling a disabled user changes
     * nothing. Throws SelfLockoutException for your own account: an admin locking themselves out
     * needs another admin to undo it.
     */
    void disable(UserId userId, UserId disabledBy);

    /** Lets the user log in again. Enabling an enabled user changes nothing. */
    void enable(UserId userId);
}
