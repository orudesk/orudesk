package com.orudesk.orudesk.identity.adapter.out.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    boolean existsByEmail(String email);

    /** Loads the user and its roles in one query (a JOIN), instead of one query each. */
    @EntityGraph(attributePaths = "roles")
    Optional<UserJpaEntity> findWithRolesById(UUID id);
}
