package com.orudesk.orudesk.identity.adapter.in.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * The refresh token as a cookie, for the web app. JavaScript can't read an HttpOnly cookie, so a script injected
 * into the page (XSS) can't steal the long-lived token; at worst it can use the 15-minute access token while the
 * page is open.
 * <p>
 * A client asks for this by sending {@value #TRANSPORT_HEADER}: cookie. Other clients (scripts, the mobile app
 * later) keep getting the refresh token in the JSON body, as in 1.0.0.
 */
@Component
class RefreshTokenCookie {

    static final String NAME = "orudesk_refresh";
    static final String TRANSPORT_HEADER = "X-Refresh-Token-Transport";
    static final String COOKIE_TRANSPORT = "cookie";
    /** Sent only to the auth endpoints, not with every API call. */
    static final String PATH = "/api/v1/auth";

    private final Duration ttl;
    private final boolean secure;

    RefreshTokenCookie(@Value("${orudesk.security.jwt.refresh-token-ttl}") Duration ttl,
                       @Value("${orudesk.security.refresh-cookie.secure:true}") boolean secure) {
        this.ttl = ttl;
        this.secure = secure;
    }

    static boolean requested(String transport) {
        return COOKIE_TRANSPORT.equalsIgnoreCase(transport);
    }

    ResponseCookie issue(String refreshToken) {
        return base(refreshToken).maxAge(ttl).build();
    }

    /** Same name, path and flags with Max-Age 0: the browser deletes the cookie. */
    ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)        // invisible to JavaScript
                .secure(secure)        // HTTPS only (off in the local profile, for plain http://localhost)
                .sameSite("Strict")    // never sent on a request that another site starts: no CSRF
                .path(PATH);
    }
}
