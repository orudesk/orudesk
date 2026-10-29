package com.orudesk.orudesk.identity.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The JSON body of POST /api/v1/auth/password-reset/confirm. The same password rules as registration. */
record ConfirmPasswordResetRequest(
        @NotBlank @Size(max = 100) String token,
        @NotBlank @Size(min = 8, max = 72) String newPassword) {

    @Override
    public String toString() {
        return "ConfirmPasswordResetRequest[token=****, newPassword=****]";
    }
}
