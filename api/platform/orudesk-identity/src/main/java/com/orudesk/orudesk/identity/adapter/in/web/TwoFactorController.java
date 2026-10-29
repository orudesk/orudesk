package com.orudesk.orudesk.identity.adapter.in.web;

import com.orudesk.orudesk.identity.application.port.in.TotpSetup;
import com.orudesk.orudesk.identity.application.port.in.TwoFactorStatus;
import com.orudesk.orudesk.identity.application.port.in.TwoFactorUseCase;
import com.orudesk.orudesk.identity.domain.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** The logged-in user's own two-factor sign-in (Day 67). Changes ask for the current password again. */
@RestController
@RequestMapping("/api/v1/users/me/two-factor")
@Tag(name = "Users")
class TwoFactorController {

    record PasswordRequest(@NotBlank @Size(max = 72) String password) {
        @Override
        public String toString() {
            return "PasswordRequest[password=****]";
        }
    }

    record CodeRequest(@NotBlank @Size(max = 20) String code) {
    }

    record RecoveryCodesResponse(List<String> recoveryCodes) {
    }

    private final TwoFactorUseCase twoFactor;

    TwoFactorController(TwoFactorUseCase twoFactor) {
        this.twoFactor = twoFactor;
    }

    @GetMapping
    @Operation(operationId = "getMyTwoFactor", summary = "Whether two-factor sign-in is on, and how many recovery codes are left")
    TwoFactorStatus status(@AuthenticationPrincipal Jwt jwt) {
        return twoFactor.status(user(jwt));
    }

    @PostMapping("/setup")
    @Operation(operationId = "beginTwoFactorSetup", summary = "Step 1: a new secret for an authenticator app")
    TotpSetup setup(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PasswordRequest request) {
        return twoFactor.beginSetup(user(jwt), request.password());
    }

    @PostMapping("/confirm")
    @Operation(operationId = "confirmTwoFactorSetup",
            summary = "Step 2: the app's first code turns two-factor sign-in on; returns the recovery codes, once")
    RecoveryCodesResponse confirm(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CodeRequest request) {
        return new RecoveryCodesResponse(twoFactor.confirmSetup(user(jwt), request.code()));
    }

    @PostMapping("/recovery-codes")
    @Operation(operationId = "newRecoveryCodes", summary = "New recovery codes; the old ones stop working")
    RecoveryCodesResponse recoveryCodes(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PasswordRequest request) {
        return new RecoveryCodesResponse(twoFactor.newRecoveryCodes(user(jwt), request.password()));
    }

    @PostMapping("/disable")
    @Operation(operationId = "disableTwoFactor", summary = "Turn two-factor sign-in off")
    @ApiResponse(responseCode = "204", description = "Off: the password alone signs in again")
    ResponseEntity<Void> disable(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PasswordRequest request) {
        twoFactor.disable(user(jwt), request.password());
        return ResponseEntity.noContent().build();
    }

    private static UserId user(Jwt jwt) {
        return new UserId(UUID.fromString(jwt.getSubject()));
    }
}
