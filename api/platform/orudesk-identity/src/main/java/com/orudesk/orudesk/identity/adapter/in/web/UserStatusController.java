package com.orudesk.orudesk.identity.adapter.in.web;

import com.orudesk.orudesk.identity.application.port.in.UserStatusUseCase;
import com.orudesk.orudesk.identity.domain.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Disable and enable a login (Day 48). Actions, so POST to a verb under the user, rather than a PATCH of an
 * "enabled" field: disabling does more than flip a field (it ends sessions), and the URL says so. Both are safe to
 * repeat: disabling a disabled user is still a success.
 */
@RestController
@RequestMapping("/api/v1/users/{userId}")
@Tag(name = "Users")
@PreAuthorize("hasRole('ADMIN')")
class UserStatusController {

    private final UserStatusUseCase userStatus;

    UserStatusController(UserStatusUseCase userStatus) {
        this.userStatus = userStatus;
    }

    @PostMapping("/disable")
    @Operation(operationId = "disableUser", summary = "Stop a user from logging in, and end their sessions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void disable(@PathVariable UUID userId, @AuthenticationPrincipal Jwt jwt) {
        userStatus.disable(new UserId(userId), new UserId(UUID.fromString(jwt.getSubject())));
    }

    @PostMapping("/enable")
    @Operation(operationId = "enableUser", summary = "Let a disabled user log in again")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void enable(@PathVariable UUID userId) {
        userStatus.enable(new UserId(userId));
    }
}
