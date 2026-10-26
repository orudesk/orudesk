package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.AssignRoleUseCase;
import com.orudesk.orudesk.identity.application.port.in.RoleGranted;
import com.orudesk.orudesk.identity.application.port.out.LoadOrgTreePort;
import com.orudesk.orudesk.identity.application.port.out.LoadRolePort;
import com.orudesk.orudesk.identity.application.port.out.LoadUserPort;
import com.orudesk.orudesk.identity.application.port.out.SaveUserPort;
import com.orudesk.orudesk.identity.domain.OrgUnitId;
import com.orudesk.orudesk.identity.domain.RoleGrant;
import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.UnknownRoleException;
import com.orudesk.orudesk.identity.domain.User;
import com.orudesk.orudesk.identity.domain.UserId;
import com.orudesk.orudesk.identity.domain.UserNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;

@Service
@Transactional
public class AssignRoleService implements AssignRoleUseCase {

    private final LoadUserPort loadUserPort;
    private final LoadRolePort loadRolePort;
    private final SaveUserPort saveUserPort;
    private final LoadOrgTreePort loadOrgTreePort;
    private final ApplicationEventPublisher events;

    public AssignRoleService(LoadUserPort loadUserPort, LoadRolePort loadRolePort, SaveUserPort saveUserPort,
                             LoadOrgTreePort loadOrgTreePort, ApplicationEventPublisher events) {
        this.loadUserPort = loadUserPort;
        this.loadRolePort = loadRolePort;
        this.saveUserPort = saveUserPort;
        this.loadOrgTreePort = loadOrgTreePort;
        this.events = events;
    }

    @Override
    @CacheEvict(cacheNames = UserProfileService.CACHE, key = "#userId.value()")   // the cached profile is now out of date
    public void assignRole(UserId userId, RoleCode role) {
        User user = loadUserAndCheckRole(userId, role);
        boolean isNew = !user.grants().contains(RoleGrant.tenantWide(role));
        user.assignRole(role);
        saveUserPort.save(user);
        if (isNew) {
            events.publishEvent(new RoleGranted(user.tenantId().value(), userId.value(), user.email().value(),
                    role.value(), null));
        }
    }

    @Override
    @CacheEvict(cacheNames = UserProfileService.CACHE, key = "#userId.value()")
    public void assignRoleAt(UserId userId, RoleCode role, OrgUnitId unit) {
        User user = loadUserAndCheckRole(userId, role);
        // The unit must exist in this tenant, or OrgUnitNotFoundException
        String unitName = loadOrgTreePort.loadTree().get(unit).name();
        boolean isNew = !user.grants().contains(RoleGrant.at(role, unit));
        user.assignRoleAt(role, unit);
        saveUserPort.save(user);
        if (isNew) {   // giving a role twice changes nothing, so there's nothing to record
            events.publishEvent(new RoleGranted(user.tenantId().value(), userId.value(), user.email().value(),
                    role.value(), unitName));
        }
    }

    private User loadUserAndCheckRole(UserId userId, RoleCode role) {
        if (!loadRolePort.roleExists(role)) {
            throw new UnknownRoleException(role);
        }
        return loadUserPort.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }
}
