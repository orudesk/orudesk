package com.orudesk.orudesk.identity.adapter.in.web;

import com.orudesk.orudesk.identity.application.port.in.AssignRoleUseCase;
import com.orudesk.orudesk.identity.application.port.in.RevokeRoleUseCase;
import com.orudesk.orudesk.identity.domain.OrgUnitId;
import com.orudesk.orudesk.identity.domain.RoleChangeNotAllowedException;
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

import java.util.Arrays;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.orudesk.orudesk.shared.adapter.in.security.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

@WebMvcTest(UserRoleController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")   // every test runs as a logged-in ADMIN, unless it says otherwise
class UserRoleControllerTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID UNIT_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID ME = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssignRoleUseCase assignRole;

    @MockitoBean
    private RevokeRoleUseCase revokeRole;

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

    // Revoking needs to know who is asking (an admin may not remove their own ADMIN role), so these tests send a
    // token with a subject rather than @WithMockUser's "user".
    private static RequestPostProcessor tokenWith(String... roles) {
        return jwt().jwt(token -> token.subject(ME.toString()))
                .authorities(Arrays.stream(roles).map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                        .toArray(SimpleGrantedAuthority[]::new));
    }

    @Test
    void revokesATenantWideRoleAndReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{id}/roles/{role}", USER_ID, "admin").with(tokenWith("ADMIN")))
                .andExpect(status().isNoContent());

        verify(revokeRole).revokeRole(new UserId(USER_ID), RoleCode.ADMIN, null, new UserId(ME));
    }

    @Test
    void anAdminOfAUnitRevokesRolesInsideIt() throws Exception {
        given(orgAccess.currentUserHasRoleAt("ADMIN", UNIT_ID)).willReturn(true);

        mockMvc.perform(delete("/api/v1/users/{id}/roles/{role}", USER_ID, "ADMIN")
                        .param("orgUnitId", UNIT_ID.toString()).with(tokenWith("USER")))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/users/{id}/roles/{role}", USER_ID, "ADMIN").with(tokenWith("USER")))
                .andExpect(status().isForbidden());   // but not tenant-wide

        verify(revokeRole).revokeRole(new UserId(USER_ID), RoleCode.ADMIN, new OrgUnitId(UNIT_ID), new UserId(ME));
    }

    @Test
    void returns409WhenTheRulesSayNo() throws Exception {
        willThrow(new RoleChangeNotAllowedException("a user must have at least one role"))
                .given(revokeRole).revokeRole(any(), any(), any(), any());

        mockMvc.perform(delete("/api/v1/users/{id}/roles/{role}", USER_ID, "USER").with(tokenWith("ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Role change not allowed"))
                .andExpect(jsonPath("$.detail").value("a user must have at least one role"));
    }
}
