package com.orudesk.orudesk.identity.adapter.in.startup;

import com.orudesk.orudesk.identity.application.port.in.AssignRoleUseCase;
import com.orudesk.orudesk.identity.application.port.in.RegisterUserCommand;
import com.orudesk.orudesk.identity.application.port.in.RegisterUserUseCase;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.EmailAlreadyRegisteredException;
import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.TenantId;
import com.orudesk.orudesk.identity.domain.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Creates the first ADMIN when the app starts, if ORUDESK_ADMIN_EMAIL and ORUDESK_ADMIN_PASSWORD are set.
 * Without it nobody could ever assign roles, because assigning roles needs an ADMIN.
 */
@Component
class FirstAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FirstAdminInitializer.class);

    private final RegisterUserUseCase registerUser;
    private final AssignRoleUseCase assignRole;
    private final TenantId defaultTenant;
    private final String email;
    private final String password;

    FirstAdminInitializer(RegisterUserUseCase registerUser,
                          AssignRoleUseCase assignRole,
                          @Value("${orudesk.identity.default-tenant-id}") UUID defaultTenantId,
                          @Value("${orudesk.identity.first-admin.email:}") String email,
                          @Value("${orudesk.identity.first-admin.password:}") String password) {
        this.registerUser = registerUser;
        this.assignRole = assignRole;
        this.defaultTenant = new TenantId(defaultTenantId);
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            log.info("No first admin configured (ORUDESK_ADMIN_EMAIL / ORUDESK_ADMIN_PASSWORD), skipping");
            return;
        }
        try {
            UserId id = registerUser.register(RegisterUserCommand.fromConfiguration(defaultTenant, new Email(email), password));
            assignRole.assignRole(id, RoleCode.ADMIN);
            log.info("Created the first admin: {}", email);
        } catch (EmailAlreadyRegisteredException _) {
            log.info("First admin {} already exists, nothing to do", email);
        }
    }
}
