package com.orudesk.orudesk.identity.application.port.out;

import com.orudesk.orudesk.identity.domain.OrgTree;

public interface LoadOrgTreePort {

    /** Every org unit of the current tenant. */
    OrgTree loadTree();
}
