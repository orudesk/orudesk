package com.orudesk.orudesk.shared;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Compares a record before and after an edit, field by field, and keeps the ones that differ:
 *
 * <pre>
 * List&lt;FieldChange&gt; changes = new FieldChanges()
 *         .compare("phone", before.phone(), after.phone())
 *         .compareHidden("pan", before.pan(), after.pan())
 *         .list();
 * </pre>
 *
 * The order of the calls is the order of the list (the order of the form, so the audit log reads naturally).
 */
public final class FieldChanges {

    private final List<FieldChange> changes = new ArrayList<>();

    public FieldChanges compare(String field, @Nullable Object before, @Nullable Object after) {
        if (!Objects.equals(unwrap(before), unwrap(after))) {
            changes.add(FieldChange.of(field, unwrap(before), unwrap(after)));
        }
        return this;
    }

    /** The same comparison, but only the field's name is kept if it changed. */
    public FieldChanges compareHidden(String field, @Nullable Object before, @Nullable Object after) {
        if (!Objects.equals(unwrap(before), unwrap(after))) {
            changes.add(FieldChange.hidden(field));
        }
        return this;
    }

    public List<FieldChange> list() {
        return List.copyOf(changes);
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }

    // Domain getters often return Optional (person.phone()); compare and record what's inside it
    private static @Nullable Object unwrap(@Nullable Object value) {
        return value instanceof Optional<?> optional ? optional.orElse(null) : value;
    }
}
