package com.orudesk.orudesk.identity.application.port.in;

/** What a successful login or refresh returns: a short-lived access token and a long-lived refresh token. */
public record AuthTokens(String accessToken, long expiresInSeconds, String refreshToken) {

    @Override
    public String toString() {
        return "AuthTokens[accessToken=****, expiresInSeconds=" + expiresInSeconds + ", refreshToken=****]";
    }
}
