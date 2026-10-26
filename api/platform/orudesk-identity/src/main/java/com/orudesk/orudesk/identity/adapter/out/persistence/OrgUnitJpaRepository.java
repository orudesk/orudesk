package com.orudesk.orudesk.identity.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface OrgUnitJpaRepository extends JpaRepository<OrgUnitJpaEntity, UUID> {
}
