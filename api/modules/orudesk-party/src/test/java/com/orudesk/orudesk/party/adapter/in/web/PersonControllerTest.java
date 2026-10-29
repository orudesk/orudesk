package com.orudesk.orudesk.party.adapter.in.web;

import com.orudesk.orudesk.party.application.port.in.EditPersonCommand;
import com.orudesk.orudesk.party.application.port.in.EditPersonUseCase;
import com.orudesk.orudesk.party.application.port.in.GetPersonQuery;
import com.orudesk.orudesk.party.application.port.in.PeopleSearch;
import com.orudesk.orudesk.party.application.port.in.PersonDetails;
import com.orudesk.orudesk.party.application.port.in.PersonPage;
import com.orudesk.orudesk.party.application.port.in.RegisterPersonCommand;
import com.orudesk.orudesk.party.application.port.in.RegisterPersonUseCase;
import com.orudesk.orudesk.party.application.port.in.SearchPeopleQuery;
import com.orudesk.orudesk.party.domain.PersonId;
import com.orudesk.orudesk.party.domain.PersonNotFoundException;
import com.orudesk.orudesk.shared.EditConflictException;
import com.orudesk.orudesk.shared.adapter.in.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PersonController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class PersonControllerTest {

    private static final UUID PERSON_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterPersonUseCase registerPerson;

    @MockitoBean
    private EditPersonUseCase editPerson;

    @MockitoBean
    private GetPersonQuery getPerson;

    @MockitoBean
    private SearchPeopleQuery searchPeople;

    @Test
    void registersAPersonAndReturns201WithItsLocation() throws Exception {
        given(registerPerson.register(new RegisterPersonCommand("Asha", "Rao", LocalDate.of(1990, 4, 23), null, null)))
                .willReturn(PERSON_ID);

        mockMvc.perform(post("/api/v1/people")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Asha", "lastName": "Rao", "dateOfBirth": "1990-04-23"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/people/" + PERSON_ID))
                .andExpect(jsonPath("$.id").value(PERSON_ID.toString()));
    }

    @Test
    void rejectsAMissingLastNameWith400() throws Exception {
        mockMvc.perform(post("/api/v1/people")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Asha"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.lastName").exists());

        verify(registerPerson, never()).register(any());
    }

    @Test
    void returnsAPerson() throws Exception {
        given(getPerson.getPerson(PERSON_ID))
                .willReturn(new PersonDetails(PERSON_ID, "Asha", "Rao", LocalDate.of(1990, 4, 23), null, null, 0));

        mockMvc.perform(get("/api/v1/people/{id}", PERSON_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Rao"))
                .andExpect(jsonPath("$.dateOfBirth").value("1990-04-23"));
    }

    @Test
    void returns404ForAnUnknownPerson() throws Exception {
        given(getPerson.getPerson(PERSON_ID)).willThrow(new PersonNotFoundException(new PersonId(PERSON_ID)));

        mockMvc.perform(get("/api/v1/people/{id}", PERSON_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Person not found"));
    }

    @Test
    void searchesWithDefaultsAndReturnsThePageAndItsCursor() throws Exception {
        given(searchPeople.search(new PeopleSearch("asha", null, 20))).willReturn(new PersonPage(
                List.of(new PersonDetails(PERSON_ID, "Asha", "Rao", null, null, null, 0)), "bmV4dA"));

        mockMvc.perform(get("/api/v1/people").param("q", "asha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].lastName").value("Rao"))
                .andExpect(jsonPath("$.nextCursor").value("bmV4dA"));
    }

    @Test
    void rejectsAPageThatIsTooBigWith400() throws Exception {
        mockMvc.perform(get("/api/v1/people").param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("size must be between 1 and 100"));

        verify(searchPeople, never()).search(any());
    }

    @Test
    void editsAPersonAndReturnsItWithTheNewVersion() throws Exception {
        given(editPerson.edit(new EditPersonCommand(PERSON_ID, 3, "Asha", "Rao", null, "+91 98450 11111", null)))
                .willReturn(new PersonDetails(PERSON_ID, "Asha", "Rao", null, "+91 98450 11111", null, 4));

        mockMvc.perform(put("/api/v1/people/{id}", PERSON_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version": 3, "firstName": "Asha", "lastName": "Rao", "phone": "+91 98450 11111"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(4))
                .andExpect(jsonPath("$.phone").value("+91 98450 11111"));
    }

    @Test
    void anEditWithoutAVersionIs400() throws Exception {
        mockMvc.perform(put("/api/v1/people/{id}", PERSON_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Asha", "lastName": "Rao"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.version").exists());

        verify(editPerson, never()).edit(any());
    }

    @Test
    void anEditOfAnOldVersionIs409WithTheCurrentVersion() throws Exception {
        given(editPerson.edit(any())).willThrow(new EditConflictException("person", PERSON_ID, 3, 5));

        mockMvc.perform(put("/api/v1/people/{id}", PERSON_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version": 3, "firstName": "Asha", "lastName": "Rao"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Edit conflict"))
                .andExpect(jsonPath("$.currentVersion").value(5));
    }

    @Test
    @WithMockUser(roles = "USER")
    void onlyAnAdminMayUseTheRegistry() throws Exception {
        mockMvc.perform(get("/api/v1/people/{id}", PERSON_ID))
                .andExpect(status().isForbidden());
    }
}
