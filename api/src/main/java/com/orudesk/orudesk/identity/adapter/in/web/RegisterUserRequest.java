package com.orudesk.orudesk.identity.adapter.in.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The JSON body of POST /api/v1/auth/register. */
record RegisterUserRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 8, max = 72) String password) {

    @Override
    public String toString() {
        return "RegisterUserRequest[email=" + email + ", password=****]";
    }
}
