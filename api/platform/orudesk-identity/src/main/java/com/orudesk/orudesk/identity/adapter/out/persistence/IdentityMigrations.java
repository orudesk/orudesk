package com.orudesk.orudesk.identity.adapter.out.persistence;

import com.orudesk.orudesk.shared.ModuleMigrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/** Identity's tables (iam_*) come first: every other module's tables point at iam_tenant. */
@Configuration
class IdentityMigrations {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    ModuleMigrations identityModuleMigrations() {
        return new ModuleMigrations("identity", "classpath:db/migration/identity");
    }
}
