package com.orudesk.orudesk.identity.adapter.in.web;

import com.orudesk.orudesk.identity.application.port.in.AuthTokens;

/** The JSON answer to login and refresh, in the shape OAuth 2 clients expect. */
record TokenResponse(String accessToken, String tokenType, long expiresIn, String refreshToken) {

    static TokenResponse from(AuthTokens tokens) {
        return new TokenResponse(tokens.accessToken(), "Bearer", tokens.expiresInSeconds(), tokens.refreshToken());
    }
}
