package com.orudesk.orudesk.shared;

import java.util.Optional;
import java.util.UUID;

/**
 * The tenant of the logged-in caller, for code that writes SQL itself (Hibernate adds the tenant to entities with
 * a @TenantId on its own). Added for the NGO's receipt numbers (its Day 57), which are counted per tenant.
 */
public interface CurrentTenant {

    /** Empty when nobody is logged in: at startup, during login, in an async event listener. */
    Optional<UUID> id();

    /** The tenant, or IllegalStateException: for code that must only run in a logged-in request. */
    default UUID require() {
        return id().orElseThrow(() -> new IllegalStateException("no tenant: this needs a logged-in user"));
    }
}
