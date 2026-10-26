package com.orudesk.orudesk.party.application.port.out;

import java.util.UUID;

/** A place in the sorted list of people: the sort key of the last person on a page. */
public record PersonPosition(String lastName, String firstName, UUID id) {
}
