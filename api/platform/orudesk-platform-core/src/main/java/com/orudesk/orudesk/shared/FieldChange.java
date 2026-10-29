package com.orudesk.orudesk.shared;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * One field of a record that an edit changed, with its value before and after, as text (Day 47). The audit log
 * keeps these, so "who changed Asha's phone number, and what was it before?" has an answer.
 *
 * @param field        the field's name as the API calls it: "phone", "dateOfBirth"
 * @param before       the old value as text; null if it had none (or the values are hidden)
 * @param after        the new value as text; null if it has none now (or the values are hidden)
 * @param valuesHidden true when only the fact of the change may be kept, not the values: a tax id, a diagnosis.
 *                     The audit log is read by admins, and a value they may not see must not leak through it.
 */
public record FieldChange(String field, @Nullable String before, @Nullable String after, boolean valuesHidden) {

    private static final int MAX_VALUE_LENGTH = 500;

    public FieldChange {
        Objects.requireNonNull(field, "field must not be null");
        before = cut(before);
        after = cut(after);
        if (valuesHidden && (before != null || after != null)) {
            throw new IllegalArgumentException("a change with hidden values must not carry the values");
        }
    }

    /** A change whose values may be kept. Values are turned into text with toString(): a LocalDate gives 1990-04-23. */
    public static FieldChange of(String field, @Nullable Object before, @Nullable Object after) {
        return new FieldChange(field, before == null ? null : before.toString(), after == null ? null : after.toString(),
                false);
    }

    /** A change that is recorded without its values. */
    public static FieldChange hidden(String field) {
        return new FieldChange(field, null, null, true);
    }

    // Like the summary: a long value is cut rather than refused, so auditing never blocks the change it records
    private static @Nullable String cut(@Nullable String value) {
        return value != null && value.length() > MAX_VALUE_LENGTH ? value.substring(0, MAX_VALUE_LENGTH - 1) + "…" : value;
    }
}
