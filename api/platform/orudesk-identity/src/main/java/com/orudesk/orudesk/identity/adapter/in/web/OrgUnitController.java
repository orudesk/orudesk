package com.orudesk.orudesk.identity.adapter.in.web;

import com.orudesk.orudesk.identity.application.port.in.CreateOrgUnitCommand;
import com.orudesk.orudesk.identity.application.port.in.CreateOrgUnitUseCase;
import com.orudesk.orudesk.identity.application.port.in.GetOrgTreeQuery;
import com.orudesk.orudesk.identity.application.port.in.OrgUnitNode;
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

    OrgUnitController(CreateOrgUnitUseCase createOrgUnit, GetOrgTreeQuery getOrgTree) {
        this.createOrgUnit = createOrgUnit;
        this.getOrgTree = getOrgTree;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
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
}
