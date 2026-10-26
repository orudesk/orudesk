package com.orudesk.orudesk.identity.adapter.in.web;

import com.orudesk.orudesk.identity.application.port.in.LoginCommand;
import com.orudesk.orudesk.identity.application.port.in.LoginUseCase;
import com.orudesk.orudesk.identity.application.port.in.LogoutUseCase;
import com.orudesk.orudesk.identity.application.port.in.RefreshTokensUseCase;
import com.orudesk.orudesk.identity.application.port.in.RegisterUserCommand;
import com.orudesk.orudesk.identity.application.port.in.RegisterUserUseCase;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.TenantId;
import com.orudesk.orudesk.identity.domain.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
@SecurityRequirements   // empty: these endpoints need no token
class AuthController {

    private final RegisterUserUseCase registerUser;
    private final LoginUseCase login;
    private final RefreshTokensUseCase refreshTokens;
    private final LogoutUseCase logout;
    private final TenantId defaultTenant;

    AuthController(RegisterUserUseCase registerUser, LoginUseCase login,
                   RefreshTokensUseCase refreshTokens, LogoutUseCase logout,
                   @Value("${orudesk.identity.default-tenant-id}") UUID defaultTenantId) {
        this.registerUser = registerUser;
        this.login = login;
        this.refreshTokens = refreshTokens;
        this.logout = logout;
        this.defaultTenant = new TenantId(defaultTenantId);
    }

    @PostMapping("/register")
    @Operation(operationId = "registerUser", summary = "Create a login (joins the default tenant)")
    ResponseEntity<RegisterUserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        UserId id = registerUser.register(
                new RegisterUserCommand(defaultTenant, new Email(request.email()), request.password()));
        return ResponseEntity
                .created(URI.create("/api/v1/users/" + id.value()))
                .body(new RegisterUserResponse(id.value()));
    }

    @PostMapping("/login")
    @Operation(operationId = "login", summary = "Exchange email and password for an access and a refresh token")
    TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return TokenResponse.from(login.login(new LoginCommand(new Email(request.email()), request.password())));
    }

    @PostMapping("/refresh")
    @Operation(operationId = "refreshTokens", summary = "Exchange a refresh token for new tokens (the old one stops working)")
    TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return TokenResponse.from(refreshTokens.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(operationId = "logout", summary = "Revoke a refresh token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@Valid @RequestBody RefreshTokenRequest request) {
        logout.logout(request.refreshToken());
    }
}
