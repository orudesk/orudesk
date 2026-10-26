package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.CreateOrgUnitCommand;
import com.orudesk.orudesk.identity.application.port.in.CreateOrgUnitUseCase;
import com.orudesk.orudesk.identity.application.port.in.GetOrgTreeQuery;
import com.orudesk.orudesk.identity.application.port.in.OrgUnitNode;
import com.orudesk.orudesk.identity.application.port.out.LoadOrgTreePort;
import com.orudesk.orudesk.identity.application.port.out.SaveOrgUnitPort;
import com.orudesk.orudesk.identity.domain.OrgTree;
import com.orudesk.orudesk.identity.domain.OrgUnit;
import com.orudesk.orudesk.identity.domain.OrgUnitCodeTakenException;
import com.orudesk.orudesk.identity.domain.OrgUnitId;
import com.orudesk.orudesk.identity.domain.OrgUnitNotFoundException;
import com.orudesk.orudesk.identity.domain.OrgUnitType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class OrgUnitService implements CreateOrgUnitUseCase, GetOrgTreeQuery {

    private final LoadOrgTreePort loadOrgTreePort;
    private final SaveOrgUnitPort saveOrgUnitPort;

    public OrgUnitService(LoadOrgTreePort loadOrgTreePort, SaveOrgUnitPort saveOrgUnitPort) {
        this.loadOrgTreePort = loadOrgTreePort;
        this.saveOrgUnitPort = saveOrgUnitPort;
    }

    @Override
    public UUID create(CreateOrgUnitCommand command) {
        OrgTree tree = loadOrgTreePort.loadTree();
        // Only this tenant's units are in the tree, so another tenant's parent is simply "not found"
        OrgUnit parent = tree.get(new OrgUnitId(command.parentId()));
        if (tree.codeTaken(command.code())) {
            throw new OrgUnitCodeTakenException(command.code());
        }
        OrgUnit unit = OrgUnit.createUnder(parent, new OrgUnitType(command.type()), command.code(), command.name());
        saveOrgUnitPort.save(unit);
        return unit.id().value();
    }

    @Override
    @Transactional(readOnly = true)
    public OrgUnitNode getTree() {
        OrgTree tree = loadOrgTreePort.loadTree();
        OrgUnit root = tree.root()
                .orElseThrow(() -> new OrgUnitNotFoundException("this tenant has no org units yet"));
        return toNode(tree, root);
    }

    private static OrgUnitNode toNode(OrgTree tree, OrgUnit unit) {
        return new OrgUnitNode(
                unit.id().value(), unit.type().value(), unit.code(), unit.name(),
                tree.childrenOf(unit.id()).stream().map(child -> toNode(tree, child)).toList());
    }
}
