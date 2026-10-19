package com.orudesk.orudesk.identity.adapter.in.web;

import com.orudesk.orudesk.identity.application.port.in.AssignRoleUseCase;
import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.UserId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
class UserRoleController {

    private final AssignRoleUseCase assignRole;

    UserRoleController(AssignRoleUseCase assignRole) {
        this.assignRole = assignRole;
    }

    @PostMapping("/{userId}/roles")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void assignRole(@PathVariable UUID userId, @Valid @RequestBody AssignRoleRequest request) {
        assignRole.assignRole(new UserId(userId), new RoleCode(request.role()));
    }
}
