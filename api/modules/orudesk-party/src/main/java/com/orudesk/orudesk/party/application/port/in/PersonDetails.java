package com.orudesk.orudesk.party.application.port.in;

import java.time.LocalDate;
import java.util.UUID;

/** A read-only view of a person. Optional values are null when unknown. */
public record PersonDetails(UUID id, String firstName, String lastName, LocalDate dateOfBirth, String phone, String email) {
}
