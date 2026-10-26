package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.out.LoadOrgTreePort;
import com.orudesk.orudesk.identity.application.port.out.SaveOrgUnitPort;
import com.orudesk.orudesk.identity.domain.OrgTree;
import com.orudesk.orudesk.identity.domain.OrgUnit;

import java.util.ArrayList;
import java.util.List;

/** A fake for unit tests: one tenant's units in a list. */
class InMemoryOrgUnitStore implements LoadOrgTreePort, SaveOrgUnitPort {

    private final List<OrgUnit> units = new ArrayList<>();

    @Override
    public OrgTree loadTree() {
        return new OrgTree(units);
    }

    @Override
    public void save(OrgUnit unit) {
        units.add(unit);
    }
}
