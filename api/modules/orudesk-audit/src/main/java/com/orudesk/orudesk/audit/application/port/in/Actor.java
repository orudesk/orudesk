package com.orudesk.orudesk.audit.application.port.in;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

/** Who did something, as far as we know: nobody logged in has a null id. */
public record Actor(@Nullable UUID id, @Nullable String email) {

    public static final Actor NOBODY = new Actor(null, null);
}
