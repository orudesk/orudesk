package com.orudesk.orudesk.shared;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.UUID;

/**
 * Runs work that no user started (a startup seed, later a scheduled job) inside one tenant, as an admin of it.
 *
 * Everything tenant-scoped (Hibernate's @TenantId, the services) finds the tenant in the logged-in user's token.
 * Rather than a second way in for background work, this gives the work a token of its own, so it goes through
 * exactly the same checks and filters as a request. The subject is the nil UUID: no real user did this.
 */
public final class SystemActor {

    private static final String SYSTEM = new UUID(0, 0).toString();

    private SystemActor() {
    }

    public static void runAs(UUID tenantId, Runnable work) {
        Jwt token = Jwt.withTokenValue("system")
                .header("alg", "none")
                .subject(SYSTEM)
                .claim("tenant_id", tenantId.toString())
                .build();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new JwtAuthenticationToken(token, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        SecurityContextHolder.setContext(context);
        try {
            work.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
