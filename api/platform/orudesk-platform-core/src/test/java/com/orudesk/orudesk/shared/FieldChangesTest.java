package com.orudesk.orudesk.shared;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FieldChangesTest {

    @Test
    void keepsOnlyTheFieldsThatDifferInTheOrderTheyWereCompared() {
        var changes = new FieldChanges()
                .compare("firstName", "Asha", "Asha")
                .compare("dateOfBirth", Optional.empty(), Optional.of(LocalDate.of(1990, 4, 23)))
                .compare("phone", Optional.of("+91 98450 00000"), Optional.empty())
                .list();

        assertThat(changes).containsExactly(
                FieldChange.of("dateOfBirth", null, "1990-04-23"),
                FieldChange.of("phone", "+91 98450 00000", null));
    }

    @Test
    void aHiddenChangeKeepsOnlyTheFieldsName() {
        var changes = new FieldChanges().compareHidden("pan", "ABCDE1234F", "ABCDE9999F").list();

        assertThat(changes).containsExactly(new FieldChange("pan", null, null, true));
        assertThatThrownBy(() -> new FieldChange("pan", "ABCDE1234F", null, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aVeryLongValueIsCutNotRefused() {
        FieldChange change = FieldChange.of("notes", "", "x".repeat(2000));

        assertThat(change.after()).hasSize(500).endsWith("…");
    }
}
