package com.orudesk.orudesk.identity.adapter.in.web;

import com.orudesk.orudesk.identity.application.port.in.CreateOrgUnitCommand;
import com.orudesk.orudesk.identity.application.port.in.CreateOrgUnitUseCase;
import com.orudesk.orudesk.identity.application.port.in.GetOrgTreeQuery;
import com.orudesk.orudesk.identity.application.port.in.OrgAccessQuery;
import com.orudesk.orudesk.identity.application.port.in.OrgUnitNode;
import com.orudesk.orudesk.identity.domain.OrgUnitCodeTakenException;
import com.orudesk.orudesk.shared.adapter.in.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrgUnitController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class OrgUnitControllerTest {

    private static final UUID ROOT = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID SCHOOL = UUID.fromString("66666666-6666-6666-6666-666666666666");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateOrgUnitUseCase createOrgUnit;

    @MockitoBean
    private GetOrgTreeQuery getOrgTree;

    @MockitoBean
    private OrgAccessQuery orgAccessQuery;

    @MockitoBean(name = "orgAccess")   // @PreAuthorize's @orgAccess, and the controller's CurrentOrgUnit
    private OrgUnitAccessAdapter orgAccess;

    @Test
    void createsAUnitAndReturns201WithItsLocation() throws Exception {
        given(orgAccess.currentUserHasRoleAt("ADMIN", ROOT)).willReturn(true);
        given(createOrgUnit.create(new CreateOrgUnitCommand(ROOT, "SCHOOL", "sunrise-a", "Sunrise School A")))
                .willReturn(SCHOOL);

        mockMvc.perform(post("/api/v1/org-units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentId": "55555555-5555-5555-5555-555555555555", "type": "SCHOOL",
                                 "code": "sunrise-a", "name": "Sunrise School A"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/org-units/" + SCHOOL));
    }

    @Test
    void aTakenCodeIs409() throws Exception {
        given(orgAccess.currentUserHasRoleAt("ADMIN", ROOT)).willReturn(true);
        given(createOrgUnit.create(any())).willThrow(new OrgUnitCodeTakenException("sunrise-a"));

        mockMvc.perform(post("/api/v1/org-units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentId": "55555555-5555-5555-5555-555555555555", "type": "SCHOOL",
                                 "code": "sunrise-a", "name": "Sunrise School A"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Org unit code taken"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void onlyAnAdminOfTheParentCreatesUnitsButEveryoneSeesTheTree() throws Exception {   // orgAccess says false
        given(getOrgTree.getTree()).willReturn(new OrgUnitNode(ROOT, "ORGANISATION", "demo", "Demo Organisation", List.of()));

        mockMvc.perform(post("/api/v1/org-units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentId": "55555555-5555-5555-5555-555555555555", "type": "SCHOOL",
                                 "code": "sunrise-a", "name": "Sunrise School A"}
                                """))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/org-units"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Demo Organisation"));

        verify(createOrgUnit, never()).create(any());
    }
}
