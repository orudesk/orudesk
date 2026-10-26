package com.orudesk.orudesk.identity.application.port.in;

import java.util.List;
import java.util.UUID;

/** One unit and everything below it: the shape a tree view on screen needs. */
public record OrgUnitNode(UUID id, String type, String code, String name, List<OrgUnitNode> children) {
}
