package com.orudesk.orudesk.identity.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, UUID> {

    boolean existsByCode(String code);

    Set<RoleJpaEntity> findByCodeIn(Collection<String> codes);

    /**
     * One statement, so two app instances starting at the same time can't both insert the role: the second
     * one's insert hits the unique code and does nothing. @return 1 if inserted, 0 if the code existed.
     */
    @Modifying
    @Query(value = """
            INSERT INTO iam_role (id, code, name, description)
            VALUES (gen_random_uuid(), :code, :name, :description)
            ON CONFLICT (code) DO NOTHING
            """, nativeQuery = true)
    int insertIfMissing(String code, String name, String description);
}
