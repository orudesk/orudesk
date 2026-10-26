package com.orudesk.orudesk.identity.adapter.out.persistence;

import com.orudesk.orudesk.identity.application.port.out.LoadOrgTreePort;
import com.orudesk.orudesk.identity.application.port.out.SaveOrgUnitPort;
import com.orudesk.orudesk.identity.domain.OrgTree;
import com.orudesk.orudesk.identity.domain.OrgUnit;
import com.orudesk.orudesk.identity.domain.OrgUnitId;
import com.orudesk.orudesk.identity.domain.OrgUnitType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class OrgUnitPersistenceAdapter implements LoadOrgTreePort, SaveOrgUnitPort {

    private final OrgUnitJpaRepository units;

    OrgUnitPersistenceAdapter(OrgUnitJpaRepository units) {
        this.units = units;
    }

    /** findAll() is filtered by @TenantId: one tenant's units, never everyone's. */
    @Override
    @Transactional(readOnly = true)
    public OrgTree loadTree() {
        return new OrgTree(units.findAll().stream()
                .map(entity -> OrgUnit.restore(
                        new OrgUnitId(entity.getId()),
                        entity.getParentId() == null ? null : new OrgUnitId(entity.getParentId()),
                        new OrgUnitType(entity.getType()),
                        entity.getCode(),
                        entity.getName()))
                .toList());
    }

    @Override
    @Transactional
    public void save(OrgUnit unit) {
        units.save(new OrgUnitJpaEntity(
                unit.id().value(),
                unit.parentId().map(OrgUnitId::value).orElse(null),
                unit.type().value(),
                unit.code(),
                unit.name()));
    }
}
