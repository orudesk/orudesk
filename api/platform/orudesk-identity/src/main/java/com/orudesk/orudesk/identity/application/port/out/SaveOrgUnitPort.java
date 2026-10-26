package com.orudesk.orudesk.identity.application.port.out;

import com.orudesk.orudesk.identity.domain.OrgUnit;

public interface SaveOrgUnitPort {

    void save(OrgUnit unit);
}
