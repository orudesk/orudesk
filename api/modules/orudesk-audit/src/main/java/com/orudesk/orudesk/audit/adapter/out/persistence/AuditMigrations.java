package com.orudesk.orudesk.audit.adapter.out.persistence;

import com.orudesk.orudesk.shared.ModuleMigrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/** After identity and party (it points at neither, but the core's modules keep a fixed order). */
@Configuration
class AuditMigrations {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 2)
    ModuleMigrations auditModuleMigrations() {
        return new ModuleMigrations("audit", "classpath:db/migration/audit");
    }
}
