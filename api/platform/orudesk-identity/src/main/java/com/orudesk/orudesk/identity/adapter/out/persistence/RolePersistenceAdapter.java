package com.orudesk.orudesk.identity.adapter.out.persistence;

import com.orudesk.orudesk.identity.application.port.out.SaveRolePort;
import com.orudesk.orudesk.identity.domain.RoleCode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class RolePersistenceAdapter implements SaveRolePort {

    private final RoleJpaRepository roles;

    RolePersistenceAdapter(RoleJpaRepository roles) {
        this.roles = roles;
    }

    @Override
    @Transactional
    public boolean addIfMissing(RoleCode code, String name, String description) {
        return roles.insertIfMissing(code.value(), name, description) == 1;
    }
}
