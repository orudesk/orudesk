package com.orudesk.orudesk.identity.adapter.out.persistence;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    boolean existsByEmail(String email);

    /** Loads the user, its grants and their roles in one query (JOINs), instead of one query each. */
    @EntityGraph(attributePaths = {"grants", "grants.role"})
    Optional<UserJpaEntity> findWithRolesById(UUID id);

    @EntityGraph(attributePaths = {"grants", "grants.role"})
    Optional<UserJpaEntity> findWithRolesByEmail(String email);

    /**
     * Step 1 of a page: just the ids, sorted and limited in the database. (Fetching the grants here too would make
     * Hibernate load EVERY matching user and cut the page in memory: a JOIN FETCH of a collection can't be
     * combined with LIMIT, because the database's rows are user-role pairs, not users.)
     * <p>The ESCAPE is needed: Hibernate writes {@code escape ''} when none is given, which turns the backslash off.
     */
    @Query("""
            SELECT u.id FROM UserJpaEntity u
            WHERE (:pattern IS NULL OR LOWER(u.email) LIKE :pattern ESCAPE '\\')
              AND (:after IS NULL OR u.email > :after)
            ORDER BY u.email
            """)
    List<UUID> findPageIds(@Nullable String pattern, @Nullable String after, Limit limit);

    /** Step 2: those users with their grants, in one query. */
    @EntityGraph(attributePaths = {"grants", "grants.role"})
    List<UserJpaEntity> findWithRolesByIdIn(Collection<UUID> ids);
}
