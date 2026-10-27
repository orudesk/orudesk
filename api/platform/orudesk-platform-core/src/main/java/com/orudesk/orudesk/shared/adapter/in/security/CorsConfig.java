package com.orudesk.orudesk.shared.adapter.in.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

/**
 * Which other web sites' pages may call this API from the browser (CORS).
 * <p>
 * By default: none. The web app is served from the same origin as the API (Vite's proxy in development, Caddy in
 * production), so the browser never asks. Set {@code ORUDESK_WEB_ALLOWED_ORIGINS=https://app.example.org} only
 * when the web app lives on another origin, such as a separate app. subdomain.
 */
@Configuration(proxyBeanMethods = false)
class CorsConfig {

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${orudesk.web.allowed-origins:}") List<String> origins) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        List<String> allowed = origins.stream().map(String::trim).filter(origin -> !origin.isEmpty()).toList();
        if (allowed.isEmpty()) {
            return source;   // no configuration for any path: cross-origin requests get no CORS headers
        }
        CorsConfiguration api = new CorsConfiguration();
        api.setAllowedOrigins(allowed);   // exact origins: never "*" together with credentials
        api.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        api.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE,
                "X-Org-Unit", "X-Refresh-Token-Transport", "X-Request-Id"));
        // So the app can read where a created thing lives, and which request it was (for a bug report)
        api.setExposedHeaders(List.of(HttpHeaders.LOCATION, "X-Request-Id"));
        api.setAllowCredentials(true);    // the refresh cookie
        api.setMaxAge(Duration.ofHours(1));   // the browser may skip the preflight for an hour
        source.registerCorsConfiguration("/api/**", api);
        return source;
    }
}
