package com.orudesk.orudesk.identity.adapter.in.web;

import com.orudesk.orudesk.identity.application.port.in.RegisterUserCommand;
import com.orudesk.orudesk.identity.application.port.in.RegisterUserUseCase;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.TenantId;
import com.orudesk.orudesk.identity.domain.UserId;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    private final RegisterUserUseCase registerUser;
    private final TenantId defaultTenant;

    AuthController(RegisterUserUseCase registerUser,
                   @Value("${orudesk.identity.default-tenant-id}") UUID defaultTenantId) {
        this.registerUser = registerUser;
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
}
