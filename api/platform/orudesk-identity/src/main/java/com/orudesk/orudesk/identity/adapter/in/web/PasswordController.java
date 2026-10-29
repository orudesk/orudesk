package com.orudesk.orudesk.identity.adapter.in.web;

import com.orudesk.orudesk.identity.application.port.in.ChangePasswordUseCase;
import com.orudesk.orudesk.identity.domain.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** The logged-in user's own password (Day 69). */
@RestController
@RequestMapping("/api/v1/users/me/password")
@Tag(name = "Users")
class PasswordController {

    /** The same length rules as registration; "password" is the current one, as in the two-factor requests. */
    record ChangePasswordRequest(@NotBlank @Size(max = 72) String password,
                                 @NotBlank @Size(min = 8, max = 72) String newPassword) {
        @Override
        public String toString() {
            return "ChangePasswordRequest[password=****, newPassword=****]";
        }
    }

    private final ChangePasswordUseCase changePassword;

    PasswordController(ChangePasswordUseCase changePassword) {
        this.changePassword = changePassword;
    }

    @PostMapping
    @Operation(operationId = "changeMyPassword", summary = "Change the password; every other session is signed out")
    @ApiResponse(responseCode = "204", description = "Changed")
    ResponseEntity<Void> change(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request) {
        changePassword.changePassword(new UserId(UUID.fromString(jwt.getSubject())), session(jwt),
                request.password(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    private static @Nullable UUID session(Jwt jwt) {
        String sid = jwt.getClaimAsString("sid");
        return sid == null ? null : UUID.fromString(sid);
    }
}
