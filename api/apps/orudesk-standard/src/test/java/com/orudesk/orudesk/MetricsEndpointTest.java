package com.orudesk.orudesk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Prometheus reads the metrics with the metrics token; nobody else can, not even an admin. */
@SpringBootTest(properties = "orudesk.metrics.token=" + MetricsEndpointTest.TOKEN)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MetricsEndpointTest {

    static final String TOKEN = "a-metrics-token-of-at-least-32-characters";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void prometheusReadsTheMetricsWithTheToken() throws Exception {
        mockMvc.perform(get("/api/health"));   // at least one request to count

        mockMvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("http_server_requests_seconds_count")))
                .andExpect(content().string(containsString("application=\"orudesk\"")));
    }

    @Test
    void withoutTheTokenThereAreNoMetrics() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer wrong"))
                .andExpect(status().isUnauthorized());
        // A user's token is the wrong kind of credential here
        mockMvc.perform(get("/actuator/prometheus").with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void everyResponseSaysWhichRequestItWas() throws Exception {
        mockMvc.perform(get("/api/health").header("X-Request-Id", "from-the-browser-1"))
                .andExpect(header().string("X-Request-Id", "from-the-browser-1"));
        mockMvc.perform(get("/api/v1/people"))   // refused, and still has an id
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-Id"));
    }
}
