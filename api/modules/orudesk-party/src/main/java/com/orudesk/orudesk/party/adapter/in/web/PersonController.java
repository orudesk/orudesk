package com.orudesk.orudesk.party.adapter.in.web;

import com.orudesk.orudesk.party.application.port.in.GetPersonQuery;
import com.orudesk.orudesk.party.application.port.in.PeopleSearch;
import com.orudesk.orudesk.party.application.port.in.PersonDetails;
import com.orudesk.orudesk.party.application.port.in.PersonPage;
import com.orudesk.orudesk.party.application.port.in.RegisterPersonCommand;
import com.orudesk.orudesk.party.application.port.in.RegisterPersonUseCase;
import com.orudesk.orudesk.party.application.port.in.SearchPeopleQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/people")
@Tag(name = "People")
@PreAuthorize("hasRole('ADMIN')")   // on the class: applies to every endpoint below
class PersonController {

    private final RegisterPersonUseCase registerPerson;
    private final GetPersonQuery getPerson;
    private final SearchPeopleQuery searchPeople;

    PersonController(RegisterPersonUseCase registerPerson, GetPersonQuery getPerson, SearchPeopleQuery searchPeople) {
        this.registerPerson = registerPerson;
        this.getPerson = getPerson;
        this.searchPeople = searchPeople;
    }

    @PostMapping
    @Operation(operationId = "registerPerson", summary = "Add a person to the registry")
    ResponseEntity<RegisterPersonResponse> register(@Valid @RequestBody RegisterPersonRequest request) {
        UUID id = registerPerson.register(new RegisterPersonCommand(
                request.firstName(), request.lastName(), request.dateOfBirth(), request.phone(), request.email()));
        return ResponseEntity
                .created(URI.create("/api/v1/people/" + id))
                .body(new RegisterPersonResponse(id));
    }

    /** GET /api/v1/people?q=asha&size=20&after=... : one page of people, sorted by name. */
    @GetMapping
    @Operation(operationId = "searchPeople", summary = "Find people by part of a name, phone or email, one page at a time")
    PersonPage search(@RequestParam(required = false) String q,
                      @RequestParam(required = false) String after,
                      @RequestParam(defaultValue = "20") int size) {
        return searchPeople.search(new PeopleSearch(q, after, size));
    }

    @GetMapping("/{personId}")
    @Operation(operationId = "getPerson", summary = "One person")
    PersonDetails get(@PathVariable UUID personId) {
        return getPerson.getPerson(personId);
    }
}
