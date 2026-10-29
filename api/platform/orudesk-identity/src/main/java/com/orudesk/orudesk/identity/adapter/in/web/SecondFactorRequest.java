package com.orudesk.orudesk.identity.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The JSON body of POST /api/v1/auth/login/two-factor. */
record SecondFactorRequest(@NotBlank @Size(max = 100) String loginToken, @NotBlank @Size(max = 20) String code) {

    @Override
    public String toString() {
        return "SecondFactorRequest[loginToken=****, code=****]";
    }
}
