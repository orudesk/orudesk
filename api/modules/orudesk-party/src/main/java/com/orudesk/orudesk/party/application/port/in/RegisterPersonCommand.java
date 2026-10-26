package com.orudesk.orudesk.party.application.port.in;

import java.time.LocalDate;

/** Everything needed to add a person. Only the names are required. */
public record RegisterPersonCommand(String firstName, String lastName, LocalDate dateOfBirth, String phone, String email) {
}
