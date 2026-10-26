package com.orudesk.orudesk.party.adapter.in.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** The JSON body of POST /api/v1/people. Dates are ISO strings: "1990-04-23". */
record RegisterPersonRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @PastOrPresent LocalDate dateOfBirth,
        @Size(max = 30) String phone,
        @Email @Size(max = 320) String email) {
}
