package com.orudesk.orudesk;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Starts a throwaway PostgreSQL and Redis in Docker for the tests, and points Spring at them.
 * Tests no longer touch your local database or Redis, and they run the same on any machine with Docker,
 * including GitHub Actions.
 *
 * Use it with @Import(TestcontainersConfiguration.class) on every test that needs a database or Redis.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /** The same major version as production. Pin it: "latest" would change under you. */
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:17");
    }

    /** No Redis class ships with Testcontainers, so a generic container: name = "redis" tells Spring what it is. */
    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redis() {
        return new GenericContainer<>("redis:7").withExposedPorts(6379);
    }
}
