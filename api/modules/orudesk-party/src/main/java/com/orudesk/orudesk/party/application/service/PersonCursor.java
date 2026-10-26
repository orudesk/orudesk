package com.orudesk.orudesk.party.application.service;

import com.orudesk.orudesk.party.application.port.out.PersonPosition;
import com.orudesk.orudesk.party.domain.Person;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * Turns a position in the list into an opaque string for clients, and back.
 * Clients must treat it as a black box, so we can change what's inside without breaking them.
 */
final class PersonCursor {

    // PostgreSQL text can never contain the NUL character, so it can't appear inside a name
    private static final String SEPARATOR = "\u0000";

    private PersonCursor() {
    }

    static String encode(Person last) {
        String raw = last.name().last() + SEPARATOR + last.name().first() + SEPARATOR + last.id().value();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    static PersonPosition decode(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = raw.split(SEPARATOR, -1);
            if (parts.length != 3) {
                throw new IllegalArgumentException("invalid cursor");
            }
            return new PersonPosition(parts[0], parts[1], UUID.fromString(parts[2]));
        } catch (IllegalArgumentException e) {   // bad Base64 or a bad UUID both land here
            throw new IllegalArgumentException("invalid cursor", e);
        }
    }
}
