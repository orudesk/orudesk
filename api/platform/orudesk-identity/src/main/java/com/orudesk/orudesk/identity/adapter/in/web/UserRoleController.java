package com.orudesk.orudesk.identity.adapter.in.web;

import com.orudesk.orudesk.identity.application.port.in.AssignRoleUseCase;
import com.orudesk.orudesk.identity.domain.OrgUnitId;
import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
class UserRoleController {

    private final AssignRoleUseCase assignRole;

    UserRoleController(AssignRoleUseCase assignRole) {
        this.assignRole = assignRole;
    }

    /**
     * A tenant ADMIN may grant any role anywhere. An ADMIN of a unit (say, Sunrise Group) may grant roles
     * inside that unit only: never tenant-wide, and never in a unit outside their own.
     */
    @PostMapping("/{userId}/roles")
    @Operation(operationId = "assignRole", summary = "Give a user a role, for the whole tenant or one org unit")
    @PreAuthorize("hasRole('ADMIN') or (#request.orgUnitId() != null and @orgAccess.currentUserHasRoleAt('ADMIN', #request.orgUnitId()))")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void assignRole(@PathVariable UUID userId, @Valid @RequestBody AssignRoleRequest request) {
        RoleCode role = new RoleCode(request.role());
        if (request.orgUnitId() == null) {
            assignRole.assignRole(new UserId(userId), role);
        } else {
            assignRole.assignRoleAt(new UserId(userId), role, new OrgUnitId(request.orgUnitId()));
        }
    }
}
