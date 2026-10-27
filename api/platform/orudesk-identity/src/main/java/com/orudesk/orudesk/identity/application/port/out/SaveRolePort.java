package com.orudesk.orudesk.identity.application.port.out;

import com.orudesk.orudesk.identity.domain.RoleCode;

public interface SaveRolePort {

    /** @return true if the role was added, false if a role with this code already existed */
    boolean addIfMissing(RoleCode code, String name, String description);
}
