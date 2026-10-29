package com.orudesk.orudesk.identity.adapter.in.web;

import com.orudesk.orudesk.identity.application.port.in.*;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.EmailAlreadyRegisteredException;
import com.orudesk.orudesk.identity.domain.InvalidCredentialsException;
import com.orudesk.orudesk.identity.domain.TooManyLoginAttemptsException;
import com.orudesk.orudesk.identity.domain.UserId;
import com.orudesk.orudesk.shared.adapter.in.security.SecurityConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, RefreshTokenCookie.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterUserUseCase registerUser;

    @MockitoBean
    private LoginUseCase login;

    @MockitoBean
    private RefreshTokensUseCase refreshTokens;

    @MockitoBean
    private LogoutUseCase logout;

    @MockitoBean
    private PasswordResetUseCase passwordReset;

    @Test
    void registersAUserAndReturns201WithItsLocation() throws Exception {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        given(registerUser.register(any(RegisterUserCommand.class))).willReturn(new UserId(id));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "karthikeyan@orudesk.com", "password": "secret-password"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/users/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void rejectsAnInvalidEmailWith400BeforeCallingTheUseCase() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "not-an-email", "password": "secret-password"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.email").exists());

        verify(registerUser, never()).register(any());
    }

    @Test
    void returns409WhenTheEmailIsTaken() throws Exception {
        given(registerUser.register(any(RegisterUserCommand.class)))
                .willThrow(new EmailAlreadyRegisteredException(new Email("karthikeyan@orudesk.com")));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "karthikeyan@orudesk.com", "password": "secret-password"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Email already registered"))
                .andExpect(jsonPath("$.detail").value("email already registered: karthikeyan@orudesk.com"));
    }

    @Test
    void loginReturnsBearerTokens() throws Exception {
        given(login.login(any(LoginCommand.class))).willReturn(new AuthTokens("the-access-token", 900, "the-refresh-token"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ck@orudesk.org", "password": "secret-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("the-access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshToken").value("the-refresh-token"));
    }

    @Test
    void aFailedLoginReturns401() throws Exception {
        given(login.login(any(LoginCommand.class))).willThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ck@orudesk.org", "password": "wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Login failed"));
    }

    @Test
    void aBlockedLoginReturns429WithRetryAfter() throws Exception {
        given(login.login(any(LoginCommand.class))).willThrow(new TooManyLoginAttemptsException(Duration.ofSeconds(754)));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ck@orudesk.org", "password": "a-guess"}
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "754"))
                .andExpect(jsonPath("$.title").value("Too many login attempts"));
    }

    @Test
    void aCookieClientGetsTheRefreshTokenAsAnHttpOnlyCookieAndNotInTheBody() throws Exception {
        given(login.login(any(LoginCommand.class))).willReturn(new AuthTokens("the-access-token", 900, "the-refresh-token"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Refresh-Token-Transport", "cookie")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ck@orudesk.org", "password": "secret-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("the-access-token"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(cookie().value("orudesk_refresh", "the-refresh-token"))
                .andExpect(cookie().httpOnly("orudesk_refresh", true))
                .andExpect(cookie().secure("orudesk_refresh", true))
                .andExpect(cookie().sameSite("orudesk_refresh", "Strict"))
                .andExpect(cookie().path("orudesk_refresh", "/api/v1/auth"))
                .andExpect(cookie().maxAge("orudesk_refresh", 14 * 24 * 3600));
    }

    @Test
    void refreshReadsTheCookieWhenThereIsNoBody() throws Exception {
        given(refreshTokens.refresh("the-old-refresh-token")).willReturn(new AuthTokens("new-access", 900, "new-refresh"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Refresh-Token-Transport", "cookie")
                        .cookie(new Cookie("orudesk_refresh", "the-old-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(cookie().value("orudesk_refresh", "new-refresh"));   // rotated: the cookie is replaced
    }

    @Test
    void theCookieAloneIsNotEnoughWithoutTheHeader() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("orudesk_refresh", "the-old-refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid refresh token"));

        verify(refreshTokens, never()).refresh(any());
    }

    @Test
    void logoutRevokesTheCookiesTokenAndDeletesTheCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("X-Refresh-Token-Transport", "cookie")
                        .cookie(new Cookie("orudesk_refresh", "the-refresh-token")))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("orudesk_refresh", 0));

        verify(logout).logout("the-refresh-token");
    }
}
