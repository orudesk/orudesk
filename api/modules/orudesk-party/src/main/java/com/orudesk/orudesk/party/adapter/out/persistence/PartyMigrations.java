package com.orudesk.orudesk.party.adapter.out.persistence;

import com.orudesk.orudesk.shared.ModuleMigrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** party_person points at iam_tenant, so this runs after identity (no @Order means "last"). */
@Configuration
class PartyMigrations {

    @Bean
    ModuleMigrations partyModuleMigrations() {
        return new ModuleMigrations("party", "classpath:db/migration/party");
    }
}
