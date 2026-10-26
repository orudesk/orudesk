package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.OrgAccessQuery;
import com.orudesk.orudesk.identity.application.port.in.UnitAccess;
import com.orudesk.orudesk.identity.application.port.out.LoadOrgTreePort;
import com.orudesk.orudesk.identity.application.port.out.LoadUserPort;
import com.orudesk.orudesk.identity.domain.OrgTree;
import com.orudesk.orudesk.identity.domain.OrgUnit;
import com.orudesk.orudesk.identity.domain.OrgUnitId;
import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.RoleGrant;
import com.orudesk.orudesk.identity.domain.User;
import com.orudesk.orudesk.identity.domain.UserId;
import com.orudesk.orudesk.identity.domain.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class OrgAccessService implements OrgAccessQuery {

    private final LoadUserPort loadUserPort;
    private final LoadOrgTreePort loadOrgTreePort;

    public OrgAccessService(LoadUserPort loadUserPort, LoadOrgTreePort loadOrgTreePort) {
        this.loadUserPort = loadUserPort;
        this.loadOrgTreePort = loadOrgTreePort;
    }

    @Override
    public UnitAccess accessAt(UserId userId, OrgUnitId unitId) {
        OrgTree tree = loadOrgTreePort.loadTree();
        OrgUnit unit = tree.get(unitId);
        User user = loadUserPort.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        // A grant counts here if it's tenant-wide, or if this unit is inside the granted unit
        var roles = user.grants().stream()
                .filter(grant -> grant.isTenantWide() || tree.isWithin(unitId, grant.scope()))
                .map(grant -> grant.role().value())
                .distinct()
                .sorted()
                .toList();
        return new UnitAccess(unit.id().value(), unit.type().value(), unit.code(), unit.name(), roles);
    }

    @Override
    public Set<OrgUnitId> unitsWithRole(UserId userId, RoleCode role) {
        OrgTree tree = loadOrgTreePort.loadTree();
        User user = loadUserPort.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        Set<OrgUnitId> units = new HashSet<>();
        for (RoleGrant grant : user.grants()) {
            if (!grant.role().equals(role)) {
                continue;
            }
            if (grant.isTenantWide()) {
                return tree.root().map(root -> tree.subtreeOf(root.id())).orElse(Set.of());
            }
            if (tree.find(grant.scope()).isPresent()) {   // a grant on a unit that no longer exists is ignored
                units.addAll(tree.subtreeOf(grant.scope()));
            }
        }
        return Set.copyOf(units);
    }
}
