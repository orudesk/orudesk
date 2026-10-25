package com.orudesk.orudesk.party.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface PersonJpaRepository extends JpaRepository<PersonJpaEntity, UUID> {
}
