package com.orudesk.orudesk.shared.adapter.out.persistence;

import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Tells Hibernate which tenant the current request belongs to: the tenant_id claim of the caller's JWT.
 * Hibernate then adds "tenant_id = ?" to every query on an entity with a @TenantId field.
 *
 * Requests without a JWT (register, login, refresh, startup) run as ROOT, which Hibernate does not filter.
 */
@Component
public class CurrentTenantResolver implements CurrentTenantIdentifierResolver<UUID>, HibernatePropertiesCustomizer {

    /** Not a real tenant: a marker meaning "no logged-in user, don't filter". */
    static final UUID ROOT = new UUID(0, 0);

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return UUID.fromString(jwt.getToken().getClaimAsString("tenant_id"));
        }
        return ROOT;
    }

    @Override
    public boolean isRoot(UUID tenantId) {
        return ROOT.equals(tenantId);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    /** Registers this resolver with Hibernate when Spring Boot builds the EntityManagerFactory. */
    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
