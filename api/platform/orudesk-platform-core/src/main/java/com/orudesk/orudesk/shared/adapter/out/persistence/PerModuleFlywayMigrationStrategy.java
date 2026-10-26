package com.orudesk.orudesk.shared.adapter.out.persistence;

import com.orudesk.orudesk.shared.ModuleMigrations;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.stereotype.Component;

/**
 * Replaces Spring Boot's single Flyway run with one run per module, each with its own folder and history table.
 * So the core and a vertical can both have a V1__ without clashing, and each module's history is its own.
 *
 * Spring Boot still creates the Flyway bean (connection, settings) and still runs migrations before JPA starts;
 * we only change what "migrate" means.
 */
@Component
class PerModuleFlywayMigrationStrategy implements FlywayMigrationStrategy {

    private static final Logger log = LoggerFactory.getLogger(PerModuleFlywayMigrationStrategy.class);

    private final ObjectProvider<ModuleMigrations> modules;

    PerModuleFlywayMigrationStrategy(ObjectProvider<ModuleMigrations> modules) {
        this.modules = modules;
    }

    @Override
    public void migrate(Flyway springsFlyway) {
        modules.orderedStream().forEach(module -> {
            Flyway flyway = Flyway.configure()
                    .configuration(springsFlyway.getConfiguration())   // same database and settings
                    .locations(module.location())
                    .table(module.historyTable())
                    // The schema already has other modules' tables. A module seen for the first time
                    // starts its history at version 0, so its own V1 still runs.
                    .baselineOnMigrate(true)
                    .baselineVersion("0")
                    .load();
            int applied = flyway.migrate().migrationsExecuted;
            log.info("Module {}: {} migration(s) applied", module.module(), applied);
        });
    }
}
