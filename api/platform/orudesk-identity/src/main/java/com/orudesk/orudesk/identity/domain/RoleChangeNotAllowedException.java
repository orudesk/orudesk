package com.orudesk.orudesk.identity.domain;

/**
 * A role change that would break a rule: a user without any role, a new role for a disabled user, or an admin
 * locking themself out. The request was understood but conflicts with the user's state, so the API says 409.
 */
public class RoleChangeNotAllowedException extends IllegalStateException {

    public RoleChangeNotAllowedException(String message) {
        super(message);
    }
}
