package com.orudesk.orudesk.party.application.service;

import com.orudesk.orudesk.party.application.port.in.PeopleSearch;
import com.orudesk.orudesk.party.application.port.in.PersonDetails;
import com.orudesk.orudesk.party.application.port.in.PersonPage;
import com.orudesk.orudesk.party.application.port.in.RegisterPersonCommand;
import com.orudesk.orudesk.party.domain.PersonNotFoundException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersonServiceTest {

    // A clock stopped at one moment: "today" is the same in every run
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-23T09:00:00Z"), ZoneOffset.UTC);

    private final InMemoryPersonStore store = new InMemoryPersonStore();
    private final PersonService service = new PersonService(store, store, store, CLOCK);

    @Test
    void registersAPersonAndReadsItBack() {
        UUID id = service.register(new RegisterPersonCommand("Asha", "Rao", LocalDate.of(1990, 4, 23), "+91 98450 00000", null));

        PersonDetails details = service.getPerson(id);

        assertThat(details.firstName()).isEqualTo("Asha");
        assertThat(details.dateOfBirth()).isEqualTo(LocalDate.of(1990, 4, 23));
        assertThat(details.email()).isNull();
    }

    @Test
    void bornTodayIsFineBornTomorrowIsNot() {
        service.register(new RegisterPersonCommand("New", "Born", LocalDate.of(2026, 10, 23), null, null));

        assertThatThrownBy(() -> service.register(new RegisterPersonCommand("Not", "Yet", LocalDate.of(2026, 10, 24), null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anUnknownIdIsNotFound() {
        assertThatThrownBy(() -> service.getPerson(UUID.randomUUID()))
                .isInstanceOf(PersonNotFoundException.class);
    }

    @Test
    void pagesThroughEveryoneInNameOrderWithoutGapsOrRepeats() {
        for (String last : List.of("Rao", "Iyer", "Khan", "Das", "Menon")) {
            service.register(new RegisterPersonCommand("Asha", last, null, null, null));
        }

        List<String> seen = new ArrayList<>();
        String cursor = null;
        int pages = 0;
        do {
            PersonPage page = service.search(new PeopleSearch(null, cursor, 2));
            page.items().forEach(person -> seen.add(person.lastName()));
            cursor = page.nextCursor();
            pages++;
        } while (cursor != null);

        assertThat(seen).containsExactly("Das", "Iyer", "Khan", "Menon", "Rao");
        assertThat(pages).isEqualTo(3);   // 2 + 2 + 1, and the last page says "no more"
    }

    @Test
    void findsPeopleByAnyPartOfTheirNameOrPhoneIgnoringCase() {
        service.register(new RegisterPersonCommand("Asha", "Rao", null, "+91 98450 00000", null));
        service.register(new RegisterPersonCommand("Ravi", "Kumar", null, null, null));

        assertThat(service.search(new PeopleSearch("RAO", null, 20)).items())
                .extracting(PersonDetails::firstName).containsExactly("Asha");
        assertThat(service.search(new PeopleSearch("98450", null, 20)).items()).hasSize(1);
        assertThat(service.search(new PeopleSearch("ravi kum", null, 20)).items()).hasSize(1);
    }

    @Test
    void refusesACursorItDidNotMake() {
        assertThatThrownBy(() -> service.search(new PeopleSearch(null, "not-a-cursor", 20)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid cursor");
    }

    @Test
    void refusesTooShortTextAndTooBigPages() {
        assertThatThrownBy(() -> new PeopleSearch("ab", null, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PeopleSearch(null, null, 101)).isInstanceOf(IllegalArgumentException.class);
    }
}
