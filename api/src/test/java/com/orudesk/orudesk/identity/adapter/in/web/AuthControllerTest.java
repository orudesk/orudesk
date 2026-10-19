package com.orudesk.orudesk.identity.adapter.in.web;

import com.orudesk.orudesk.identity.application.port.in.RegisterUserCommand;
import com.orudesk.orudesk.identity.application.port.in.RegisterUserUseCase;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.EmailAlreadyRegisteredException;
import com.orudesk.orudesk.identity.domain.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tests only the web layer: JSON in, status and JSON out. The use case is a Mockito mock. */
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterUserUseCase registerUser;

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
}
