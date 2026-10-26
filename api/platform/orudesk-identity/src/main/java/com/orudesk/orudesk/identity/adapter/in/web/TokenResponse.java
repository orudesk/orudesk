package com.orudesk.orudesk.identity.adapter.in.web;

import com.orudesk.orudesk.identity.application.port.in.AuthTokens;
import org.jspecify.annotations.Nullable;

/**
 * The JSON answer to login and refresh, in the shape OAuth 2 clients expect.
 *
 * @param refreshToken null when the client asked for the refresh token as a cookie (see {@link RefreshTokenCookie})
 */
record TokenResponse(String accessToken, String tokenType, long expiresIn, @Nullable String refreshToken) {

    static TokenResponse from(AuthTokens tokens) {
        return new TokenResponse(tokens.accessToken(), "Bearer", tokens.expiresInSeconds(), tokens.refreshToken());
    }

    /** For a cookie client: the refresh token travels in the Set-Cookie header, never in JSON that scripts can read. */
    static TokenResponse withoutRefreshToken(AuthTokens tokens) {
        return new TokenResponse(tokens.accessToken(), "Bearer", tokens.expiresInSeconds(), null);
    }
}
