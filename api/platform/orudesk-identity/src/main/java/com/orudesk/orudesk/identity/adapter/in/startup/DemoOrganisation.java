package com.orudesk.orudesk.identity.adapter.in.startup;

import com.orudesk.orudesk.identity.application.port.in.AssignRoleUseCase;
import com.orudesk.orudesk.identity.application.port.in.CreateOrgUnitCommand;
import com.orudesk.orudesk.identity.application.port.in.CreateOrgUnitUseCase;
import com.orudesk.orudesk.identity.application.port.in.RegisterUserCommand;
import com.orudesk.orudesk.identity.application.port.in.RegisterUserUseCase;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.OrgUnitCodeTakenException;
import com.orudesk.orudesk.identity.domain.OrgUnitId;
import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.TenantId;
import com.orudesk.orudesk.identity.domain.UserId;
import com.orudesk.orudesk.shared.SystemActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Sample org units and users for trying Orudesk, with the "demo" profile only. Each module seeds its own data
 * (the people registry seeds people, a vertical its donors), so the seeds respect module boundaries like
 * everything else. Through the use cases, not SQL: the sample data obeys the same rules as typed-in data.
 * Runs once: on the next start the North School is already there.
 */
@Component
@Profile("demo")
class DemoOrganisation {

    /** Every demo user's password. Shown in the README: it's a demo. */
    static final String PASSWORD = "demo-password";
    private static final Logger log = LoggerFactory.getLogger(DemoOrganisation.class);
    private static final UUID ROOT_UNIT = UUID.fromString("00000000-0000-0000-0000-000000000201");

    private final CreateOrgUnitUseCase createOrgUnit;
    private final RegisterUserUseCase registerUser;
    private final AssignRoleUseCase assignRole;
    private final TenantId tenant;

    DemoOrganisation(CreateOrgUnitUseCase createOrgUnit, RegisterUserUseCase registerUser,
                     AssignRoleUseCase assignRole,
                     @Value("${orudesk.identity.default-tenant-id}") UUID tenantId) {
        this.createOrgUnit = createOrgUnit;
        this.registerUser = registerUser;
        this.assignRole = assignRole;
        this.tenant = new TenantId(tenantId);
    }

    /** ApplicationReadyEvent: after every ApplicationRunner, so the first admin exists already. */
    @EventListener(ApplicationReadyEvent.class)
    void load() {
        SystemActor.runAs(tenant.value(), () -> {
            try {
                UUID north = unit("SCHOOL", "north", "North School");
                UUID south = unit("SCHOOL", "south", "South School");
                unit("CLINIC", "riverside", "Riverside Clinic");
                assignRole.assignRoleAt(user("principal@demo.orudesk.org"), RoleCode.ADMIN, new OrgUnitId(north));
                assignRole.assignRoleAt(user("south.admin@demo.orudesk.org"), RoleCode.ADMIN, new OrgUnitId(south));
                user("office@demo.orudesk.org");
                log.info("Demo organisation loaded: 3 org units, 3 users with the password '{}'", PASSWORD);
            } catch (OrgUnitCodeTakenException _) {
                log.info("Demo organisation is already there, nothing to do");
            }
        });
    }

    private UUID unit(String type, String code, String name) {
        return createOrgUnit.create(new CreateOrgUnitCommand(ROOT_UNIT, type, code, name));
    }

    private UserId user(String email) {
        return registerUser.register(RegisterUserCommand.fromConfiguration(tenant, new Email(email), PASSWORD));
    }
}
