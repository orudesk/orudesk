package com.orudesk.orudesk.party.application.service;

import com.orudesk.orudesk.party.application.port.in.PersonDetails;
import com.orudesk.orudesk.party.application.port.in.RegisterPersonCommand;
import com.orudesk.orudesk.party.domain.PersonNotFoundException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersonServiceTest {

    // A clock stopped at one moment: "today" is the same in every run
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-23T09:00:00Z"), ZoneOffset.UTC);

    private final InMemoryPersonStore store = new InMemoryPersonStore();
    private final PersonService service = new PersonService(store, store, CLOCK);

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
}
