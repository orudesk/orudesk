package com.orudesk.orudesk.shared;

/**
 * A module's database migrations: where its SQL files are, and the name of its own Flyway history table.
 * Each module declares one as a bean. Give the bean an @Order when another module's tables point at yours.
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
