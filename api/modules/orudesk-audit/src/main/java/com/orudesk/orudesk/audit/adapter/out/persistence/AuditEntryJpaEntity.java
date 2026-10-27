package com.orudesk.orudesk.audit.adapter.out.persistence;

import com.orudesk.orudesk.audit.domain.AuditEntry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

/** One row of audit_entry. @Immutable: Hibernate never sends an UPDATE for it (the database would refuse). */
@Entity
@Immutable
@Table(name = "audit_entry")
class AuditEntryJpaEntity {

    @Id
    private UUID id;

    @TenantId   // set here when nobody is logged in (a sign-up), else checked against the user's tenant
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_email", length = 320)
    private String actorEmail;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(name = "subject_type", nullable = false, length = 32)
    private String subjectType;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(name = "request_id", length = 64)
    private String requestId;

    protected AuditEntryJpaEntity() {
        // JPA needs a no-argument constructor
    }

    static AuditEntryJpaEntity from(AuditEntry entry) {
        AuditEntryJpaEntity row = new AuditEntryJpaEntity();
        row.id = entry.id();
        row.tenantId = entry.tenantId();
        row.occurredAt = entry.occurredAt();
        row.actorId = entry.actorId();
        row.actorEmail = entry.actorEmail();
        row.action = entry.action();
        row.subjectType = entry.subjectType();
        row.subjectId = entry.subjectId();
        row.summary = entry.summary();
        row.requestId = entry.requestId();
        return row;
    }

    AuditEntry toDomain() {
        return new AuditEntry(id, tenantId, occurredAt, actorId, actorEmail, action, subjectType, subjectId,
                summary, requestId);
    }
}
