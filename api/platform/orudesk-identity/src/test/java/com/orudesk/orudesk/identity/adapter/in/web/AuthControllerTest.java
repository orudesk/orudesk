package com.orudesk.orudesk.identity.adapter.in.web;

import com.orudesk.orudesk.identity.application.port.in.*;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.EmailAlreadyRegisteredException;
import com.orudesk.orudesk.identity.domain.InvalidCredentialsException;
import com.orudesk.orudesk.identity.domain.UserId;
import com.orudesk.orudesk.shared.adapter.in.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
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
}
