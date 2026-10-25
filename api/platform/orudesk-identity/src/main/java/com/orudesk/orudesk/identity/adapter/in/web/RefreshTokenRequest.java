package com.orudesk.orudesk.identity.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

/** The JSON body of POST /api/v1/auth/refresh and /logout. */
record RefreshTokenRequest(@NotBlank String refreshToken) {

    @Override
    public String toString() {
        return "RefreshTokenRequest[refreshToken=****]";
    }
}
