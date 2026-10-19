package com.orudesk.orudesk.identity.adapter.in.web;

import com.orudesk.orudesk.identity.application.port.in.AssignRoleUseCase;
import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.UnknownRoleException;
import com.orudesk.orudesk.identity.domain.UserId;
import com.orudesk.orudesk.identity.domain.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserRoleController.class)
class UserRoleControllerTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssignRoleUseCase assignRole;

    @Test
    void assignsARoleAndReturns204() throws Exception {
        mockMvc.perform(post("/api/v1/users/{id}/roles", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role": "admin"}
                                """))
                .andExpect(status().isNoContent());

        verify(assignRole).assignRole(new UserId(USER_ID), RoleCode.ADMIN);
    }

    @Test
    void returns404ForAnUnknownUser() throws Exception {
        willThrow(new UserNotFoundException(new UserId(USER_ID)))
                .given(assignRole).assignRole(new UserId(USER_ID), RoleCode.ADMIN);

        mockMvc.perform(post("/api/v1/users/{id}/roles", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role": "ADMIN"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("User not found"));
    }

    @Test
    void returns400ForAnUnknownRole() throws Exception {
        willThrow(new UnknownRoleException(new RoleCode("PILOT")))
                .given(assignRole).assignRole(new UserId(USER_ID), new RoleCode("PILOT"));

        mockMvc.perform(post("/api/v1/users/{id}/roles", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role": "PILOT"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Unknown role"));
    }

    @Test
    void returns400ForABadlyFormattedRole() throws Exception {
        mockMvc.perform(post("/api/v1/users/{id}/roles", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role": "not a role!"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid input"));
    }
}
