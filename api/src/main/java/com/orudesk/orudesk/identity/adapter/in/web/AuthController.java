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
    ResponseEntity<RegisterUserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        UserId id = registerUser.register(
                new RegisterUserCommand(defaultTenant, new Email(request.email()), request.password()));
        return ResponseEntity
                .created(URI.create("/api/v1/users/" + id.value()))
                .body(new RegisterUserResponse(id.value()));
    }

    @PostMapping("/login")
    TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return TokenResponse.from(login.login(new LoginCommand(new Email(request.email()), request.password())));
    }

    @PostMapping("/refresh")
    TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return TokenResponse.from(refreshTokens.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@Valid @RequestBody RefreshTokenRequest request) {
        logout.logout(request.refreshToken());
    }
}
