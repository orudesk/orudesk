package com.orudesk.orudesk.identity.application.port.in;

import java.util.UUID;

/** Add a unit (a school, a branch) below an existing one. */
public record CreateOrgUnitCommand(UUID parentId, String type, String code, String name) {
}
