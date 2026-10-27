package com.orudesk.orudesk;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** With the demo profile, the app starts with sample units, users and people in the demo tenant, once. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Import(TestcontainersConfiguration.class)
class DemoDataTest {

    private static final String DEMO = "00000000-0000-0000-0000-000000000001";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConfigurableApplicationContext context;

    @Test
    void theDemoTenantHasUnitsUsersAndPeopleAndASecondStartAddsNothing() throws Exception {
        // A second start: publish the event again
        context.publishEvent(new ApplicationReadyEvent(new SpringApplication(), new String[0], context, Duration.ZERO));

        assertThat(count("SELECT count(*) FROM iam_org_unit WHERE code IN ('north', 'south', 'riverside') "
                + "AND tenant_id = '" + DEMO + "'")).isEqualTo(3);
        assertThat(count("SELECT count(*) FROM iam_user WHERE email LIKE '%@demo.orudesk.org'")).isEqualTo(3);
        assertThat(count("SELECT count(*) FROM party_person WHERE tenant_id = '" + DEMO + "'")).isEqualTo(12);

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"principal@demo.orudesk.org\", \"password\": \"demo-password\"}"))
                .andExpect(status().isOk());
    }

    // Other tests may share this database, and some count people or units: remove the sample data again
    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM iam_user WHERE email LIKE '%@demo.orudesk.org'");
        jdbc.update("DELETE FROM iam_org_unit WHERE code IN ('north', 'south', 'riverside')");
        jdbc.update("DELETE FROM party_person WHERE tenant_id = '" + DEMO + "'");
    }

    private int count(String sql) {
        Integer n = jdbc.queryForObject(sql, Integer.class);
        return n == null ? 0 : n;
    }
}
