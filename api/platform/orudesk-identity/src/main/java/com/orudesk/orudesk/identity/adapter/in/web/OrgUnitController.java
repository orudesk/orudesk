package com.orudesk.orudesk.identity.adapter.in.web;

import com.orudesk.orudesk.identity.CurrentOrgUnit;
import com.orudesk.orudesk.identity.application.port.in.CreateOrgUnitCommand;
import com.orudesk.orudesk.identity.application.port.in.CreateOrgUnitUseCase;
import com.orudesk.orudesk.identity.application.port.in.GetOrgTreeQuery;
import com.orudesk.orudesk.identity.application.port.in.OrgAccessQuery;
import com.orudesk.orudesk.identity.application.port.in.OrgUnitNode;
import com.orudesk.orudesk.identity.application.port.in.UnitAccess;
import com.orudesk.orudesk.identity.domain.OrgUnitId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Org units")
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
    @Operation(operationId = "createOrgUnit", summary = "Add a school, branch or group below an existing unit")
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
    @Operation(operationId = "getOrgTree", summary = "The tenant's org units, as a tree")
    OrgUnitNode tree() {
        return getOrgTree.getTree();
    }

    /** The unit in the X-Org-Unit header, and the caller's roles there. */
    @GetMapping("/current")
    @Operation(operationId = "getCurrentOrgUnit", summary = "The unit in the X-Org-Unit header, and your roles there")
    @Parameter(in = ParameterIn.HEADER, name = CurrentOrgUnit.HEADER, required = true,
            description = "The id of the org unit this request is for", schema = @Schema(type = "string", format = "uuid"))
    UnitAccess current() {
        return orgAccess.accessAt(OrgUnitAccessAdapter.currentUser(), new OrgUnitId(currentOrgUnit.require()));
    }
}
