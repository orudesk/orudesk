package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.AssignRoleUseCase;
import com.orudesk.orudesk.identity.application.port.out.LoadOrgTreePort;
import com.orudesk.orudesk.identity.application.port.out.LoadRolePort;
import com.orudesk.orudesk.identity.application.port.out.LoadUserPort;
import com.orudesk.orudesk.identity.application.port.out.SaveUserPort;
import com.orudesk.orudesk.identity.domain.OrgUnitId;
import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.UnknownRoleException;
import com.orudesk.orudesk.identity.domain.User;
import com.orudesk.orudesk.identity.domain.UserId;
import com.orudesk.orudesk.identity.domain.UserNotFoundException;
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

    public AssignRoleService(LoadUserPort loadUserPort, LoadRolePort loadRolePort, SaveUserPort saveUserPort,
                             LoadOrgTreePort loadOrgTreePort) {
        this.loadUserPort = loadUserPort;
        this.loadRolePort = loadRolePort;
        this.saveUserPort = saveUserPort;
        this.loadOrgTreePort = loadOrgTreePort;
    }

    @Override
    @CacheEvict(cacheNames = UserProfileService.CACHE, key = "#userId.value()")   // the cached profile is now out of date
    public void assignRole(UserId userId, RoleCode role) {
        User user = loadUserAndCheckRole(userId, role);
        user.assignRole(role);
        saveUserPort.save(user);
    }

    @Override
    @CacheEvict(cacheNames = UserProfileService.CACHE, key = "#userId.value()")
    public void assignRoleAt(UserId userId, RoleCode role, OrgUnitId unit) {
        User user = loadUserAndCheckRole(userId, role);
        loadOrgTreePort.loadTree().get(unit);   // the unit must exist in this tenant, or OrgUnitNotFoundException
        user.assignRoleAt(role, unit);
        saveUserPort.save(user);
    }

    private User loadUserAndCheckRole(UserId userId, RoleCode role) {
        if (!loadRolePort.roleExists(role)) {
            throw new UnknownRoleException(role);
        }
        return loadUserPort.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }
}
