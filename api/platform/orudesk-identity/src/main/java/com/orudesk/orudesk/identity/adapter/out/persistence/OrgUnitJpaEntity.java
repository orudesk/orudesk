package com.orudesk.orudesk.identity.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

/** One row of iam_org_unit. The parent is a plain id: the tree is built in OrgTree, not by JPA. */
@Entity
@Table(name = "iam_org_unit")
class OrgUnitJpaEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    protected OrgUnitJpaEntity() {
        // JPA needs a no-argument constructor
    }

    OrgUnitJpaEntity(UUID id, UUID parentId, String type, String code, String name) {
        this.id = id;
        this.parentId = parentId;
        this.type = type;
        this.code = code;
        this.name = name;
    }

    UUID getId() { return id; }
    UUID getParentId() { return parentId; }
    String getType() { return type; }
    String getCode() { return code; }
    String getName() { return name; }
}
