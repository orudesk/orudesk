package com.orudesk.orudesk.identity.adapter.in.startup;

import com.orudesk.orudesk.identity.RoleDefinition;
import com.orudesk.orudesk.identity.application.port.out.SaveRolePort;
import com.orudesk.orudesk.identity.domain.RoleCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Adds every role that a module declared as a {@link RoleDefinition} bean, when the app starts.
 * Runs before FirstAdminInitializer, so a role always exists before anyone can be given it.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class DeclaredRolesInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DeclaredRolesInitializer.class);

    private final ObjectProvider<RoleDefinition> definitions;
    private final SaveRolePort roles;

    DeclaredRolesInitializer(ObjectProvider<RoleDefinition> definitions, SaveRolePort roles) {
        this.definitions = definitions;
        this.roles = roles;
    }

    @Override
    public void run(ApplicationArguments args) {
        definitions.orderedStream().forEach(role -> {
            if (roles.addIfMissing(new RoleCode(role.code()), role.name(), role.description())) {
                log.info("Added role {}", role.code());
            }
        });
    }
}
