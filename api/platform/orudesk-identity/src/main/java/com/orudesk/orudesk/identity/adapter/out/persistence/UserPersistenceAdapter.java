package com.orudesk.orudesk.identity.adapter.out.persistence;

import com.orudesk.orudesk.identity.application.port.out.LoadRolePort;
import com.orudesk.orudesk.identity.application.port.out.LoadUserPort;
import com.orudesk.orudesk.identity.application.port.out.SaveUserPort;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.OrgUnitId;
import com.orudesk.orudesk.identity.domain.PasswordHash;
import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.RoleGrant;
import com.orudesk.orudesk.identity.domain.TenantId;
import com.orudesk.orudesk.identity.domain.UnknownRoleException;
import com.orudesk.orudesk.identity.domain.User;
import com.orudesk.orudesk.identity.domain.UserId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implements the identity ports with JPA. Translates between the domain User
 * and the UserJpaEntity, so neither side knows about the other.
 */
@Component
class UserPersistenceAdapter implements LoadUserPort, SaveUserPort, LoadRolePort {

    private final UserJpaRepository users;
    private final RoleJpaRepository roles;

    UserPersistenceAdapter(UserJpaRepository users, RoleJpaRepository roles) {
        this.users = users;
        this.roles = roles;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UserId id) {
        return users.findWithRolesById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(Email email) {
        return users.existsByEmail(email.value());
    }

    @Override
    @Transactional
    public void save(User user) {
        users.save(toEntity(user));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean roleExists(RoleCode code) {
        return roles.existsByCode(code.value());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(Email email) {
        return users.findWithRolesByEmail(email.value()).map(this::toDomain);
    }

    private User toDomain(UserJpaEntity entity) {
        Set<RoleGrant> grants = entity.getGrants().stream()
                .map(grant -> new RoleGrant(
                        new RoleCode(grant.getRole().getCode()),
                        grant.getOrgUnitId() == null ? null : new OrgUnitId(grant.getOrgUnitId())))
                .collect(Collectors.toSet());
        return User.restore(
                new UserId(entity.getId()),
                new TenantId(entity.getTenantId()),
                new Email(entity.getEmail()),
                new PasswordHash(entity.getPasswordHash()),
                entity.isEnabled(),
                grants);
    }

    private UserJpaEntity toEntity(User user) {
        Set<String> codes = user.grants().stream().map(grant -> grant.role().value()).collect(Collectors.toSet());
        Map<String, RoleJpaEntity> roleEntities = roles.findByCodeIn(codes).stream()
                .collect(Collectors.toMap(RoleJpaEntity::getCode, Function.identity()));
        codes.stream().filter(code -> !roleEntities.containsKey(code)).findFirst().ifPresent(missing -> {
            throw new UnknownRoleException(new RoleCode(missing));
        });
        Set<RoleGrantEmbeddable> grants = user.grants().stream()
                .map(grant -> new RoleGrantEmbeddable(
                        roleEntities.get(grant.role().value()),
                        grant.scopeId().map(OrgUnitId::value).orElse(null)))
                .collect(Collectors.toSet());
        return new UserJpaEntity(
                user.id().value(),
                user.tenantId().value(),
                user.email().value(),
                user.passwordHash().value(),
                user.isEnabled(),
                grants);
    }
}
