package com.orudesk.orudesk.identity.adapter.in.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** The JSON body of POST /api/v1/auth/login. */
record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {

    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=****]";
    }
}
