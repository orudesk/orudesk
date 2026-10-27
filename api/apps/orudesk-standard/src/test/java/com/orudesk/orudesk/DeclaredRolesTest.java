package com.orudesk.orudesk;

import com.orudesk.orudesk.identity.RoleDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** A vertical declares a role as a bean; identity adds it once, however often the app starts. */
@SpringBootTest
@Import({TestcontainersConfiguration.class, DeclaredRolesTest.ARole.class})
class DeclaredRolesTest {

    @TestConfiguration
    static class ARole {
        @Bean
        RoleDefinition testerRole() {
            return new RoleDefinition("TESTER", "Tester", "Only exists in this test");
        }
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private Map<String, ApplicationRunner> runners;

    @Test
    void aDeclaredRoleIsAddedAtStartup() {
        assertThat(jdbc.queryForObject("SELECT name FROM iam_role WHERE code = 'TESTER'", String.class))
                .isEqualTo("Tester");
    }

    @Test
    void startingAgainDoesNotAddItTwice() throws Exception {
        runners.get("declaredRolesInitializer").run(new DefaultApplicationArguments());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM iam_role WHERE code = 'TESTER'", Integer.class))
                .isEqualTo(1);
    }
}
