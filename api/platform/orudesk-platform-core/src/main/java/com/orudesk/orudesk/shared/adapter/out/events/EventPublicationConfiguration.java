package com.orudesk.orudesk.shared.adapter.out.events;

import com.orudesk.orudesk.shared.ModuleMigrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The event publication registry (Day 51): Spring Modulith's JDBC starter does the work; this adds the table's
 * migration and turns on scheduling for the housekeeping that goes with it.
 */
@Configuration
@EnableScheduling
class EventPublicationConfiguration {

    /** No @Order: the table has no foreign keys, so it doesn't matter when it's created, only that it is. */
    @Bean
    ModuleMigrations eventsModuleMigrations() {
        return new ModuleMigrations("events", "classpath:db/migration/events");
    }
}
