package com.orudesk.orudesk.identity.adapter.in.web;

import com.orudesk.orudesk.identity.CurrentOrgUnit;
import com.orudesk.orudesk.identity.application.port.in.CreateOrgUnitCommand;
import com.orudesk.orudesk.identity.application.port.in.CreateOrgUnitUseCase;
import com.orudesk.orudesk.identity.application.port.in.GetOrgTreeQuery;
import com.orudesk.orudesk.identity.application.port.in.OrgAccessQuery;
import com.orudesk.orudesk.identity.application.port.in.OrgUnitNode;
import com.orudesk.orudesk.identity.application.port.in.UnitAccess;
import com.orudesk.orudesk.identity.domain.OrgUnitId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/org-units")
class OrgUnitController {

    private final CreateOrgUnitUseCase createOrgUnit;
    private final GetOrgTreeQuery getOrgTree;
    private final OrgAccessQuery orgAccess;
    private final CurrentOrgUnit currentOrgUnit;

    OrgUnitController(CreateOrgUnitUseCase createOrgUnit, GetOrgTreeQuery getOrgTree,
                      OrgAccessQuery orgAccess, CurrentOrgUnit currentOrgUnit) {
        this.createOrgUnit = createOrgUnit;
        this.getOrgTree = getOrgTree;
        this.orgAccess = orgAccess;
        this.currentOrgUnit = currentOrgUnit;
    }

    /** An ADMIN of the parent unit (or of anything above it, or tenant-wide) may add a unit below it. */
    @PostMapping
    @PreAuthorize("@orgAccess.currentUserHasRoleAt('ADMIN', #request.parentId())")
    ResponseEntity<CreateOrgUnitResponse> create(@Valid @RequestBody CreateOrgUnitRequest request) {
        UUID id = createOrgUnit.create(new CreateOrgUnitCommand(
                request.parentId(), request.type(), request.code(), request.name()));
        return ResponseEntity
                .created(URI.create("/api/v1/org-units/" + id))
                .body(new CreateOrgUnitResponse(id));
    }

    /** Any logged-in user may see how their organisation is structured; the web app's unit switcher needs it. */
    @GetMapping
    OrgUnitNode tree() {
        return getOrgTree.getTree();
    }

    /** The unit in the X-Org-Unit header, and the caller's roles there. */
    @GetMapping("/current")
    UnitAccess current() {
        return orgAccess.accessAt(OrgUnitAccessAdapter.currentUser(), new OrgUnitId(currentOrgUnit.require()));
    }
}
