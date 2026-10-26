package com.orudesk.orudesk.identity.application.port.in;

import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.UserId;

public interface AssignRoleUseCase {

    void assignRole(UserId userId, RoleCode role);
}
