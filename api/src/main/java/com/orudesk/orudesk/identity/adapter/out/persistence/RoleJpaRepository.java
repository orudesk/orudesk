package com.orudesk.orudesk.identity.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, UUID> {

    boolean existsByCode(String code);

    Set<RoleJpaEntity> findByCodeIn(Collection<String> codes);
}
