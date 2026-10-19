package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.out.LoadRolePort;
import com.orudesk.orudesk.identity.application.port.out.LoadUserPort;
import com.orudesk.orudesk.identity.application.port.out.SaveUserPort;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.User;
import com.orudesk.orudesk.identity.domain.UserId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** A fake database for tests: keeps users in a Map. Knows the roles ADMIN and USER. */
class InMemoryUserStore implements LoadUserPort, SaveUserPort, LoadRolePort {

    private final Map<UserId, User> users = new HashMap<>();
    private final Set<RoleCode> roles = Set.of(RoleCode.ADMIN, RoleCode.USER);

    @Override
    public Optional<User> findById(UserId id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public boolean existsByEmail(Email email) {
        return users.values().stream().anyMatch(user -> user.email().equals(email));
    }

    @Override
    public void save(User user) {
        users.put(user.id(), user);
    }

    @Override
    public boolean roleExists(RoleCode code) {
        return roles.contains(code);
    }
}
