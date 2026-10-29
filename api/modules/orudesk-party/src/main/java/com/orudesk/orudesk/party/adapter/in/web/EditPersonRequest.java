package com.orudesk.orudesk.party.adapter.in.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

/**
 * The JSON body of PUT /api/v1/people/{id}: every field, as the person should now be, plus the version the
 * client read. A Long, so a missing version is a 400 rather than a silent 0.
 */
record EditPersonRequest(
        @NotNull @PositiveOrZero Long version,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @PastOrPresent @Nullable LocalDate dateOfBirth,
        @Size(max = 30) @Nullable String phone,
        @Email @Size(max = 320) @Nullable String email) {
}
