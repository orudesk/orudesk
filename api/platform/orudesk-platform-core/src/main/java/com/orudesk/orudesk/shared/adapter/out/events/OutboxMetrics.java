package com.orudesk.orudesk.shared.adapter.out.events;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Two gauges for the outbox (Day 54), read when Prometheus scrapes:
 * <ul>
 *   <li>{@code orudesk_events_incomplete}: publications not completed yet. Rising for long = listeners are behind.</li>
 *   <li>{@code orudesk_events_failed}: of those, the ones that failed. Above zero for an hour = alert someone.</li>
 * </ul>
 * One indexed COUNT per gauge per scrape (every 15-60 s): cheap, as long as the table holds work, not history
 * (completed rows are deleted after a week, Day 51).
 */
@Component
class OutboxMetrics implements SmartInitializingSingleton {

    private final ObjectProvider<MeterRegistry> registries;
    private final JdbcTemplate jdbc;

    /** An ObjectProvider: an app without actuator has no MeterRegistry, and then there's nothing to register. */
    OutboxMetrics(ObjectProvider<MeterRegistry> registries, JdbcTemplate jdbc) {
        this.registries = registries;
        this.jdbc = jdbc;
    }

    /** Registered after startup, so a scrape never runs the queries before Flyway has created the table. */
    @Override
    public void afterSingletonsInstantiated() {
        registries.ifAvailable(this::register);
    }

    private void register(MeterRegistry registry) {
        Gauge.builder("orudesk.events.incomplete", () -> count("completion_date IS NULL"))
                .description("Event publications not completed yet")
                .register(registry);
        Gauge.builder("orudesk.events.failed", () -> count("completion_date IS NULL AND status = 'FAILED'"))
                .description("Event publications whose listener failed and that wait for a retry or a person")
                .register(registry);
    }

    private double count(String condition) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM event_publication WHERE " + condition, Long.class);
        return count == null ? 0 : count;
    }
}
