package com.orudesk.orudesk.identity;

import java.util.Set;
import java.util.UUID;

/**
 * What the logged-in user may do, and where. Part of identity's public API (its top-level package),
 * so other modules and the verticals can check scoped roles without knowing how grants are stored.
 */
public interface OrgUnitAccess {

    /** True if the user has the role at this unit: granted there, at a unit above it, or tenant-wide. */
    boolean currentUserHasRoleAt(String role, UUID orgUnitId);

    /** Every unit where the user has the role: granted units and everything below them. */
    Set<UUID> unitsWhereCurrentUserHas(String role);
}
