package com.orudesk.orudesk.shared.adapter.in.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * Who may read /actuator/prometheus: whoever sends "Authorization: Bearer <ORUDESK_METRICS_TOKEN>", which is how
 * Prometheus authenticates a scrape. The metrics name endpoints and show traffic, so they're not public; and a
 * scraper is not a user, so it gets a shared secret rather than a login. No token configured: nobody may.
 *
 * A filter chain of its own, ahead of the main one, because this path uses a different kind of credential.
 */
@Configuration
class MetricsSecurityConfig {

    static final String PATH = "/actuator/prometheus";

    private final byte[] token;

    MetricsSecurityConfig(@Value("${orudesk.metrics.token:}") String token) {
        this.token = ("Bearer " + token).getBytes(StandardCharsets.UTF_8);
        if (!token.isEmpty() && token.length() < 32) {
            throw new IllegalStateException("orudesk.metrics.token must be at least 32 characters, or empty");
        }
    }

    @Bean
    @Order(1)
    SecurityFilterChain metricsFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(PATH)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().access((authentication, context) ->
                                new AuthorizationDecision(hasToken(context.getRequest()))))
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(UNAUTHORIZED))
                        .accessDeniedHandler((request, response, denied) -> response.sendError(401)));
        return http.build();
    }

    private boolean hasToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        // "Bearer " alone means no token was configured: never a match
        return header != null && token.length > "Bearer ".length()
                // Constant time: comparing byte by byte and stopping early would let an attacker time the guesses
                && MessageDigest.isEqual(header.getBytes(StandardCharsets.UTF_8), token);
    }
}
