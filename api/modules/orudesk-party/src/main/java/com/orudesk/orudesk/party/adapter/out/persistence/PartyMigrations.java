package com.orudesk.orudesk.party.adapter.out.persistence;

import com.orudesk.orudesk.shared.ModuleMigrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * party_person points at iam_tenant, so this runs after identity. And the verticals' tables point at
 * party_person, so it must run before them: they keep the default order, which is "last".
 */
@Configuration
class PartyMigrations {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    ModuleMigrations partyModuleMigrations() {
        return new ModuleMigrations("party", "classpath:db/migration/party");
    }
}
