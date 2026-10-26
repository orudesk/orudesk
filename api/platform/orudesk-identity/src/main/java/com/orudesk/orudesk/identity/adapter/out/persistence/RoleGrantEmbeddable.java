package com.orudesk.orudesk.identity.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.util.Objects;
import java.util.UUID;

/**
 * One row of iam_user_role, as part of its user: a role, and the unit it applies to (null = whole tenant).
 * Not an entity of its own: it has no life outside its user, so JPA saves and loads it with the user.
 */
@Embeddable
class RoleGrantEmbeddable {

    @ManyToOne(optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleJpaEntity role;

    @Column(name = "org_unit_id")
    private UUID orgUnitId;

    protected RoleGrantEmbeddable() {
        // JPA needs a no-argument constructor
    }

    RoleGrantEmbeddable(RoleJpaEntity role, UUID orgUnitId) {
        this.role = role;
        this.orgUnitId = orgUnitId;
    }

    RoleJpaEntity getRole() { return role; }
    UUID getOrgUnitId() { return orgUnitId; }

    // Grants live in a Set, so two grants of the same role at the same unit must be equal
    @Override
    public boolean equals(Object other) {
        return other instanceof RoleGrantEmbeddable that
                && role.getId().equals(that.role.getId())
                && Objects.equals(orgUnitId, that.orgUnitId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role.getId(), orgUnitId);
    }
}
