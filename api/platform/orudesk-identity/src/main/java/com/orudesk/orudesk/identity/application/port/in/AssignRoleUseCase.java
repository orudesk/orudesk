package com.orudesk.orudesk.identity.application.port.in;

import com.orudesk.orudesk.identity.domain.OrgUnitId;
import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.UserId;

public interface AssignRoleUseCase {

    /** The role everywhere in the user's tenant. */
    void assignRole(UserId userId, RoleCode role);

    /** The role in one org unit and everything below it. */
    void assignRoleAt(UserId userId, RoleCode role, OrgUnitId unit);
}
