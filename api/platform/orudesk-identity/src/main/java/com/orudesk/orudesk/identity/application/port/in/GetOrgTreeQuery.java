package com.orudesk.orudesk.identity.application.port.in;

public interface GetOrgTreeQuery {

    /** The logged-in user's tenant, from its root unit down. */
    OrgUnitNode getTree();
}
