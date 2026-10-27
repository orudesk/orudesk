package com.orudesk.orudesk.identity.application.port.in;

import com.orudesk.orudesk.identity.domain.OrgUnitId;
import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.UserId;
import org.jspecify.annotations.Nullable;

public interface RevokeRoleUseCase {

    /**
     * Takes one grant away: the tenant-wide role when {@code unit} is null, otherwise the role at that unit.
     * {@code revokedBy} is the admin doing it, who may not take away their own tenant-wide ADMIN role: the
     * tenant would risk having no admin left, and nobody could undo it.
     */
    void revokeRole(UserId userId, RoleCode role, @Nullable OrgUnitId unit, UserId revokedBy);
}
