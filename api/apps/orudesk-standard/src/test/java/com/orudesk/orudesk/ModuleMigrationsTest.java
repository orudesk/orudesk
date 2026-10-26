package com.orudesk.orudesk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** Each module has its own Flyway history table, and Spring Boot's shared one is never created. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ModuleMigrationsTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void identityHasItsOwnHistory() {
        assertThat(jdbc.queryForList("SELECT version FROM flyway_history_identity WHERE success ORDER BY installed_rank",
                String.class)).startsWith("1", "2", "3", "4");
    }

    @Test
    void partyStartsAtABaselineBecauseIdentityCameFirst() {
        // Version 0 is the baseline Flyway wrote because the schema already had identity's tables.
        assertThat(jdbc.queryForList("SELECT version FROM flyway_history_party WHERE success ORDER BY installed_rank",
                String.class)).startsWith("0", "1");
    }

    @Test
    void thereIsNoSharedHistoryTable() {
        Integer tables = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = current_schema() AND table_name = 'flyway_schema_history'
                """, Integer.class);
        assertThat(tables).isZero();
    }
}
