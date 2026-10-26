package com.orudesk.orudesk.identity;

/**
 * A role a module needs, such as DONOR_MANAGER. Declare one as a bean and identity adds it to the role list
 * at startup, if it isn't there yet. Part of identity's public API, so the verticals can add roles without
 * writing to identity's tables.
 *
 * @param code        upper case letters, digits and _, such as DONOR_MANAGER: what @PreAuthorize checks
 * @param name        shown to people, such as "Donor manager"
 * @param description what the role may do
 */
public record RoleDefinition(String code, String name, String description) {
}
