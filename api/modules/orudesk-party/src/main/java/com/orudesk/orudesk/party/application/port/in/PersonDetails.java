package com.orudesk.orudesk.party.application.port.in;

import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A read-only view of a person. Optional values are null when unknown.
 *
 * @param version send it back with an edit (Day 46): the edit is refused if someone changed the person since
 */
public record PersonDetails(UUID id, String firstName, String lastName, @Nullable LocalDate dateOfBirth,
                            @Nullable String phone, @Nullable String email, long version) {
}
