package com.orudesk.orudesk.party.application.port.in;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A person's new details, all of them (a replace, like PUT: a field left out is cleared, not kept).
 *
 * @param version the version the user read before making the change, from PersonDetails.version()
 */
public record EditPersonCommand(UUID personId, long version, String firstName, String lastName, LocalDate dateOfBirth,
                                String phone, String email) {
}
