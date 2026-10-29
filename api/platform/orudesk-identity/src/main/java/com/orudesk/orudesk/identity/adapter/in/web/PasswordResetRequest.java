package com.orudesk.orudesk.identity.adapter.in.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The JSON body of POST /api/v1/auth/password-reset. */
record PasswordResetRequest(@NotBlank @Email @Size(max = 320) String email) {
}
