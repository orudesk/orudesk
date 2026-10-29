package com.orudesk.orudesk.audit.adapter.out.persistence;

import com.orudesk.orudesk.shared.FieldChange;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** One row of audit_entry_change: a value with no identity of its own, owned by its audit entry. */
@Embeddable
class FieldChangeEmbeddable {

    @Column(nullable = false, length = 64)
    private String field;

    @Column(name = "old_value", length = 500)
    private String oldValue;

    @Column(name = "new_value", length = 500)
    private String newValue;

    @Column(name = "values_hidden", nullable = false)
    private boolean valuesHidden;

    protected FieldChangeEmbeddable() {
        // JPA needs a no-argument constructor
    }

    static FieldChangeEmbeddable from(FieldChange change) {
        FieldChangeEmbeddable row = new FieldChangeEmbeddable();
        row.field = change.field();
        row.oldValue = change.before();
        row.newValue = change.after();
        row.valuesHidden = change.valuesHidden();
        return row;
    }

    FieldChange toDomain() {
        return new FieldChange(field, oldValue, newValue, valuesHidden);
    }
}
