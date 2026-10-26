package com.orudesk.orudesk.identity.adapter.out.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    boolean existsByEmail(String email);

    /** Loads the user, its grants and their roles in one query (JOINs), instead of one query each. */
    @EntityGraph(attributePaths = {"grants", "grants.role"})
    Optional<UserJpaEntity> findWithRolesById(UUID id);

    @EntityGraph(attributePaths = {"grants", "grants.role"})
    Optional<UserJpaEntity> findWithRolesByEmail(String email);
}
