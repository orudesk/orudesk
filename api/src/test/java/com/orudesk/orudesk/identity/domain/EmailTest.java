package com.orudesk.orudesk.identity.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void normalisesToTrimmedLowerCase() {
        Email email = new Email("  CK@Orudesk.ORG ");
        assertThat(email.value()).isEqualTo("ck@orudesk.org");
    }

    @Test
    void twoEmailsWithTheSameValueAreEqual() {
        assertThat(new Email("a@b.com")).isEqualTo(new Email("A@B.com"));
    }

    @Test
    void rejectsTextWithoutAtSign() {
        assertThatThrownBy(() -> new Email("not-an-email"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid email");
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> new Email(null))
                .isInstanceOf(NullPointerException.class);
    }
}