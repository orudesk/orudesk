package com.orudesk.orudesk.shared;

/**
 * A module's database migrations: where its SQL files are, and the name of its own Flyway history table.
 * Each module declares one as a bean, and they run in @Order: identity first (HIGHEST_PRECEDENCE), then the
 * other core modules (HIGHEST_PRECEDENCE + 1, + 2...), then the verticals' modules. A vertical's module can
 * leave out @Order (it runs last), or use @Order(0), @Order(1)... when its modules point at each other.
 *
 * It sits in the top package of "shared", not in a sub-package: for Spring Modulith, the top package is the
 * module's public API, the only part other modules may use.
 *
 * @param module   short name, used in the history table: "identity" → flyway_history_identity
 * @param location where the module's V1__..., V2__... files are, e.g. "classpath:db/migration/identity"
 */
public record ModuleMigrations(String module, String location) {

    public ModuleMigrations {
        if (!module.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException("module must be lower case letters, digits or _, was: " + module);
        }
    }

    public String historyTable() {
        return "flyway_history_" + module;
    }
}
