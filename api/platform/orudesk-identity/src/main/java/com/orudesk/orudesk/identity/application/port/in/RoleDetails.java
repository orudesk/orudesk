package com.orudesk.orudesk.identity.application.port.in;

import org.jspecify.annotations.Nullable;

/** A role that can be granted: its code (what the API uses), and a name and description for people. */
public record RoleDetails(String code, String name, @Nullable String description) {
}
