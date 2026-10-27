package com.orudesk.orudesk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * What Docker and Kubernetes ask the app: "are you alive?" and "may I send you traffic?". Both answer without a
 * token, and say nothing more than UP or DOWN. Nothing else under /actuator is reachable over HTTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ActuatorProbesTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void livenessAndReadinessAnswerWithoutATokenAndWithoutDetails() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());   // no database names for strangers
    }

    @Test
    void endpointsThatLeakSecretsAreNotExposedEvenToAnAdmin() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/env").with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/heapdump").with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isNotFound());
    }
}
