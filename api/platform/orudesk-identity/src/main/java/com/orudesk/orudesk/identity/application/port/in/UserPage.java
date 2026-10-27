package com.orudesk.orudesk.identity.application.port.in;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * One page of users, sorted by email.
 *
 * @param nextCursor pass it as "after" to get the next page; null when this is the last page
 */
public record UserPage(List<UserSummary> items, @Nullable String nextCursor) {
}
