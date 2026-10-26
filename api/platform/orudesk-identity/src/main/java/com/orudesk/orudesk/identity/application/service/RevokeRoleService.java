package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.RevokeRoleUseCase;
import com.orudesk.orudesk.identity.application.port.in.RoleRevoked;
import com.orudesk.orudesk.identity.application.port.out.LoadOrgTreePort;
import com.orudesk.orudesk.identity.application.port.out.LoadUserPort;
import com.orudesk.orudesk.identity.application.port.out.SaveUserPort;
import com.orudesk.orudesk.identity.domain.OrgUnit;
import com.orudesk.orudesk.identity.domain.OrgUnitId;
import com.orudesk.orudesk.identity.domain.RoleChangeNotAllowedException;
import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.RoleGrant;
import com.orudesk.orudesk.identity.domain.User;
import com.orudesk.orudesk.identity.domain.UserId;
import com.orudesk.orudesk.identity.domain.UserNotFoundException;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RevokeRoleService implements RevokeRoleUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final LoadOrgTreePort loadOrgTreePort;
    private final ApplicationEventPublisher events;

    public RevokeRoleService(LoadUserPort loadUserPort, SaveUserPort saveUserPort, LoadOrgTreePort loadOrgTreePort,
                             ApplicationEventPublisher events) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.loadOrgTreePort = loadOrgTreePort;
        this.events = events;
    }

    @Override
    @CacheEvict(cacheNames = UserProfileService.CACHE, key = "#userId.value()")
    public void revokeRole(UserId userId, RoleCode role, @Nullable OrgUnitId unit, UserId revokedBy) {
        RoleGrant grant = unit == null ? RoleGrant.tenantWide(role) : RoleGrant.at(role, unit);
        if (userId.equals(revokedBy) && grant.equals(RoleGrant.tenantWide(RoleCode.ADMIN))) {
            throw new RoleChangeNotAllowedException("you cannot remove your own ADMIN role; ask another admin");
        }
        User user = loadUserPort.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (!user.grants().contains(grant)) {
            return;   // nothing to take away: still a success (DELETE is idempotent), and nothing to record
        }
        user.revoke(grant);
        saveUserPort.save(user);
        String unitName = unit == null ? null
                : loadOrgTreePort.loadTree().find(unit).map(OrgUnit::name).orElse("a unit that no longer exists");
        events.publishEvent(new RoleRevoked(user.tenantId().value(), userId.value(), user.email().value(),
                role.value(), unitName));
    }
}
