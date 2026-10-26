package com.orudesk.orudesk.party.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersonTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 23);

    @Test
    void aNewPersonHasAnIdAndTrimmedNames() {
        Person person = Person.register(new PersonName("  Asha ", " Rao"), LocalDate.of(1990, 4, 23), "", null, TODAY);

        assertThat(person.id()).isNotNull();
        assertThat(person.name()).isEqualTo(new PersonName("Asha", "Rao"));
        assertThat(person.phone()).isEmpty();   // blank means "not given"
    }

    @Test
    void namesAreRequired() {
        assertThatThrownBy(() -> new PersonName(" ", "Rao"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("first name must not be blank");
    }

    @Test
    void aDateOfBirthCannotBeInTheFuture() {
        assertThatThrownBy(() -> Person.register(new PersonName("Asha", "Rao"), TODAY.plusDays(1), null, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("date of birth must not be in the future");
    }
}
