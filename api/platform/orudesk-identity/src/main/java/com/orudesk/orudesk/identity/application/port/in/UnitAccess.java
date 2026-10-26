package com.orudesk.orudesk.identity.application.port.in;

import java.util.List;
import java.util.UUID;

/** One unit, and the roles a user has there. What the web app needs to show or hide buttons. */
public record UnitAccess(UUID id, String type, String code, String name, List<String> roles) {
}
