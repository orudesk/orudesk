package com.orudesk.orudesk.identity.adapter.in.web;

import com.orudesk.orudesk.identity.application.port.in.AuthTokens;
import com.orudesk.orudesk.identity.application.port.in.LoginCommand;
import com.orudesk.orudesk.identity.application.port.in.LoginUseCase;
import com.orudesk.orudesk.identity.application.port.in.LogoutUseCase;
import com.orudesk.orudesk.identity.application.port.in.RefreshTokensUseCase;
import com.orudesk.orudesk.identity.application.port.in.RegisterUserCommand;
import com.orudesk.orudesk.identity.application.port.in.RegisterUserUseCase;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.InvalidRefreshTokenException;
import com.orudesk.orudesk.identity.domain.TenantId;
import com.orudesk.orudesk.identity.domain.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
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
    private final RefreshTokenCookie cookie;
    private final TenantId defaultTenant;

    AuthController(RegisterUserUseCase registerUser, LoginUseCase login,
                   RefreshTokensUseCase refreshTokens, LogoutUseCase logout, RefreshTokenCookie cookie,
                   @Value("${orudesk.identity.default-tenant-id}") UUID defaultTenantId) {
        this.registerUser = registerUser;
        this.login = login;
        this.refreshTokens = refreshTokens;
        this.logout = logout;
        this.cookie = cookie;
        this.defaultTenant = new TenantId(defaultTenantId);
    }

    /** Documents the X-Refresh-Token-Transport header in the OpenAPI contract, once for the three endpoints. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Parameter(in = ParameterIn.HEADER, name = RefreshTokenCookie.TRANSPORT_HEADER,
            description = "Send 'cookie' to get and send the refresh token as an HttpOnly cookie instead of in JSON",
            schema = @Schema(type = "string", allowableValues = RefreshTokenCookie.COOKIE_TRANSPORT))
    @interface TransportHeader {
    }

    @PostMapping("/register")
    @Operation(operationId = "registerUser", summary = "Create a login (joins the default tenant)")
    @ApiResponse(responseCode = "201", description = "Created", useReturnTypeSchema = true)
    ResponseEntity<RegisterUserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        UserId id = registerUser.register(
                new RegisterUserCommand(defaultTenant, new Email(request.email()), request.password()));
        return ResponseEntity
                .created(URI.create("/api/v1/users/" + id.value()))
                .body(new RegisterUserResponse(id.value()));
    }

    @PostMapping("/login")
    @Operation(operationId = "login", summary = "Exchange email and password for an access and a refresh token")
    @TransportHeader
    ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request,
                                        @RequestHeader(name = RefreshTokenCookie.TRANSPORT_HEADER, required = false)
                                        @Nullable String transport) {
        AuthTokens tokens = login.login(new LoginCommand(new Email(request.email()), request.password()));
        return tokens(tokens, transport);
    }

    /** The refresh token comes from the JSON body or, for a cookie client, from the cookie. */
    @PostMapping("/refresh")
    @Operation(operationId = "refreshTokens", summary = "Exchange a refresh token for new tokens (the old one stops working)")
    @TransportHeader
    ResponseEntity<TokenResponse> refresh(@Valid @RequestBody(required = false) @Nullable RefreshTokenRequest request,
                                          @Parameter(hidden = true)   // the browser sends it by itself
                                          @CookieValue(name = RefreshTokenCookie.NAME, required = false)
                                          @Nullable String cookieToken,
                                          @RequestHeader(name = RefreshTokenCookie.TRANSPORT_HEADER, required = false)
                                          @Nullable String transport) {
        String refreshToken = refreshToken(request, cookieToken, transport);
        return tokens(refreshTokens.refresh(refreshToken), transport);
    }

    @PostMapping("/logout")
    @Operation(operationId = "logout", summary = "Revoke a refresh token")
    @ApiResponse(responseCode = "204", description = "Logged out")
    @TransportHeader
    ResponseEntity<Void> logout(@Valid @RequestBody(required = false) @Nullable RefreshTokenRequest request,
                                @Parameter(hidden = true)
                                @CookieValue(name = RefreshTokenCookie.NAME, required = false) @Nullable String cookieToken,
                                @RequestHeader(name = RefreshTokenCookie.TRANSPORT_HEADER, required = false)
                                @Nullable String transport) {
        logout.logout(refreshToken(request, cookieToken, transport));
        ResponseEntity.HeadersBuilder<?> response = ResponseEntity.noContent();
        if (RefreshTokenCookie.requested(transport)) {
            response.header(HttpHeaders.SET_COOKIE, cookie.clear().toString());
        }
        return response.build();
    }

    private ResponseEntity<TokenResponse> tokens(AuthTokens tokens, @Nullable String transport) {
        if (!RefreshTokenCookie.requested(transport)) {
            return ResponseEntity.ok(TokenResponse.from(tokens));
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.issue(tokens.refreshToken()).toString())
                .body(TokenResponse.withoutRefreshToken(tokens));
    }

    /**
     * The cookie counts only when the client also sends the transport header. A custom header can't be added to a
     * cross-site form post, and makes a cross-origin fetch ask permission first (CORS preflight): a second lock
     * against CSRF, after SameSite=Strict.
     */
    private static String refreshToken(@Nullable RefreshTokenRequest request, @Nullable String cookieToken,
                                       @Nullable String transport) {
        if (request != null) {
            return request.refreshToken();
        }
        if (RefreshTokenCookie.requested(transport) && cookieToken != null && !cookieToken.isBlank()) {
            return cookieToken;
        }
        throw new InvalidRefreshTokenException();
    }
}
