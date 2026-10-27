package com.orudesk.orudesk.identity.application.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** The position in the user list (the last email on the page), as an opaque string for clients. */
final class UserCursor {

    private UserCursor() {
    }

    static String encode(String email) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(email.getBytes(StandardCharsets.UTF_8));
    }

    static String decode(String cursor) {
        try {
            return new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid cursor", e);
        }
    }
}
