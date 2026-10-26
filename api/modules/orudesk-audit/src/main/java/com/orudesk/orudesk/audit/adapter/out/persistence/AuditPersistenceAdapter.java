package com.orudesk.orudesk.audit.adapter.out.persistence;

import com.orudesk.orudesk.audit.application.port.out.AuditEntryStore;
import com.orudesk.orudesk.audit.application.port.out.AuditPosition;
import com.orudesk.orudesk.audit.domain.AuditEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
class AuditPersistenceAdapter implements AuditEntryStore {

    private final EntityManager entityManager;

    AuditPersistenceAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void append(AuditEntry entry) {
        entityManager.persist(AuditEntryJpaEntity.from(entry));
    }

    /**
     * Keyset paging, newest first: "older than the last one shown". Built from only the filters that are set,
     * because PostgreSQL can't tell the type of a parameter that is only compared with NULL (":x IS NULL").
     * The tenant filter is added by Hibernate (@TenantId).
     */
    @Override
    public List<AuditEntry> findNewest(@Nullable UUID subjectId, @Nullable AuditPosition before, int limit) {
        StringBuilder jpql = new StringBuilder("SELECT e FROM AuditEntryJpaEntity e WHERE 1 = 1");
        if (subjectId != null) {
            jpql.append(" AND e.subjectId = :subjectId");
        }
        if (before != null) {
            jpql.append(" AND (e.occurredAt < :time OR (e.occurredAt = :time AND e.id < :id))");
        }
        jpql.append(" ORDER BY e.occurredAt DESC, e.id DESC");

        TypedQuery<AuditEntryJpaEntity> query = entityManager.createQuery(jpql.toString(), AuditEntryJpaEntity.class);
        if (subjectId != null) {
            query.setParameter("subjectId", subjectId);
        }
        if (before != null) {
            query.setParameter("time", before.occurredAt());
            query.setParameter("id", before.id());
        }
        return query.setMaxResults(limit).getResultList().stream().map(AuditEntryJpaEntity::toDomain).toList();
    }
}
