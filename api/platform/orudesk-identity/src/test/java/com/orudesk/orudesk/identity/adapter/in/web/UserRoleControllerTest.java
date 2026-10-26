package com.orudesk.orudesk.identity.adapter.in.web;

import com.orudesk.orudesk.identity.application.port.in.AssignRoleUseCase;
import com.orudesk.orudesk.identity.domain.OrgUnitId;
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

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.orudesk.orudesk.shared.adapter.in.security.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

@WebMvcTest(UserRoleController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")   // every test runs as a logged-in ADMIN, unless it says otherwise
class UserRoleControllerTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID UNIT_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssignRoleUseCase assignRole;

    // The bean @PreAuthorize calls as @orgAccess. A slice test doesn't create it, so it's a mock here.
    @MockitoBean(name = "orgAccess")
    private OrgUnitAccessAdapter orgAccess;

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

    @Test
    @WithMockUser(roles = "USER")
    void returns403ForAUserWhoIsNotAnAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/users/{id}/roles", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role": "ADMIN"}
                                """))
                .andExpect(status().isForbidden());

        verify(assignRole, never()).assignRole(any(), any());
    }

    @Test
    @WithMockUser(roles = "USER")
    void anAdminOfAUnitGrantsRolesInsideIt() throws Exception {
        given(orgAccess.currentUserHasRoleAt("ADMIN", UNIT_ID)).willReturn(true);

        mockMvc.perform(post("/api/v1/users/{id}/roles", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"ADMIN\", \"orgUnitId\": \"" + UNIT_ID + "\"}"))
                .andExpect(status().isNoContent());

        verify(assignRole).assignRoleAt(new UserId(USER_ID), RoleCode.ADMIN, new OrgUnitId(UNIT_ID));
    }

    @Test
    @WithMockUser(roles = "USER")
    void butNeverTenantWideAndNeverOutsideTheirUnit() throws Exception {
        mockMvc.perform(post("/api/v1/users/{id}/roles", USER_ID)   // no unit: tenant-wide, needs a tenant ADMIN
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"ADMIN\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/users/{id}/roles", USER_ID)   // orgAccess (a mock) says false by default
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"ADMIN\", \"orgUnitId\": \"" + UNIT_ID + "\"}"))
                .andExpect(status().isForbidden());

        verify(assignRole, never()).assignRoleAt(any(), any(), any());
    }
}
