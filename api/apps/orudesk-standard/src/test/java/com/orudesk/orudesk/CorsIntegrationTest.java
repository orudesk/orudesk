package com.orudesk.orudesk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The browser's question before a cross-origin call (the "preflight"): may a page from this origin send this
 * method with these headers? Only the configured origin gets a yes, and with credentials, for the refresh cookie.
 */
@SpringBootTest(properties = "orudesk.web.allowed-origins=https://app.example.org")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CorsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void theConfiguredOriginMayCallTheApiWithCredentials() throws Exception {
        mockMvc.perform(options("/api/v1/auth/refresh")
                        .header("Origin", "https://app.example.org")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "x-refresh-token-transport"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://app.example.org"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void anyOtherOriginIsRefused() throws Exception {
        mockMvc.perform(options("/api/v1/people")
                        .header("Origin", "https://evil.example.com")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
