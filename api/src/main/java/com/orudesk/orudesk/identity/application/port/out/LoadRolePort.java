package com.orudesk.orudesk.identity.application.port.out;

import com.orudesk.orudesk.identity.domain.RoleCode;

public interface LoadRolePort {

    boolean roleExists(RoleCode code);
}
