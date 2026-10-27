package com.orudesk.orudesk.audit.application.port.in;

import org.jspecify.annotations.Nullable;

import java.util.List;

/** One page of entries, newest first; nextCursor is null on the last page. */
public record AuditPage(List<AuditEntryDetails> items, @Nullable String nextCursor) {
}
