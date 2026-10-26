package com.orudesk.orudesk.identity.application.port.in;

import com.orudesk.orudesk.identity.domain.OrgUnitId;
import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.UserId;

import java.util.Set;

/** Answers "which roles does this user have here?" from their grants and the org tree. */
public interface OrgAccessQuery {

    /** @throws com.orudesk.orudesk.identity.domain.OrgUnitNotFoundException if the tenant has no such unit */
    UnitAccess accessAt(UserId user, OrgUnitId unit);

    Set<OrgUnitId> unitsWithRole(UserId user, RoleCode role);
}
