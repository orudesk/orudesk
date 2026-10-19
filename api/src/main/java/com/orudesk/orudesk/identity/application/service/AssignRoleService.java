package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.AssignRoleUseCase;
import com.orudesk.orudesk.identity.application.port.out.LoadRolePort;
import com.orudesk.orudesk.identity.application.port.out.LoadUserPort;
import com.orudesk.orudesk.identity.application.port.out.SaveUserPort;
import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.UnknownRoleException;
import com.orudesk.orudesk.identity.domain.User;
import com.orudesk.orudesk.identity.domain.UserId;
import com.orudesk.orudesk.identity.domain.UserNotFoundException;

public class AssignRoleService implements AssignRoleUseCase {

    private final LoadUserPort loadUserPort;
    private final LoadRolePort loadRolePort;
    private final SaveUserPort saveUserPort;

    public AssignRoleService(LoadUserPort loadUserPort, LoadRolePort loadRolePort, SaveUserPort saveUserPort) {
        this.loadUserPort = loadUserPort;
        this.loadRolePort = loadRolePort;
        this.saveUserPort = saveUserPort;
    }

    @Override
    public void assignRole(UserId userId, RoleCode role) {
        if (!loadRolePort.roleExists(role)) {
            throw new UnknownRoleException(role);
        }
        User user = loadUserPort.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        user.assignRole(role);
        saveUserPort.save(user);
    }
}
