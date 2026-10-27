package com.orudesk.orudesk.identity.application.port.in;

import com.orudesk.orudesk.shared.AuditableEvent;

import java.util.Locale;
import java.util.UUID;

/** A new unit in the tenant's tree: a school, a clinic, a department. */
public record OrgUnitCreated(UUID unitId, String type, String name, String parentName) implements AuditableEvent {

    @Override public String action() { return "identity.org-unit.created"; }
    @Override public String subjectType() { return "org-unit"; }
    @Override public UUID subjectId() { return unitId; }
    @Override public String summary() { return "Created " + type.toLowerCase(Locale.ROOT) + " " + name + " under " + parentName; }
}
