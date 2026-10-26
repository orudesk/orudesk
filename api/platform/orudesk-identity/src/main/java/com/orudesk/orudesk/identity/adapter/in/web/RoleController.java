package com.orudesk.orudesk.identity.adapter.in.web;

import com.orudesk.orudesk.identity.application.port.in.ListRolesQuery;
import com.orudesk.orudesk.identity.application.port.in.RoleDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Roles")
class RoleController {

    private final ListRolesQuery listRoles;

    RoleController(ListRolesQuery listRoles) {
        this.listRoles = listRoles;
    }

    /** Role definitions aren't secret: any logged-in user may see what each role is called. */
    @GetMapping
    @Operation(operationId = "listRoles", summary = "Every role that can be granted")
    List<RoleDetails> list() {
        return listRoles.listRoles();
    }
}
