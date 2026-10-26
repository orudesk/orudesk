package com.orudesk.orudesk.party.application.port.in;

import java.util.List;

/**
 * One page of people.
 *
 * @param nextCursor pass it as "after" to get the next page; null when this is the last page
 */
public record PersonPage(List<PersonDetails> items, String nextCursor) {
}
