package com.orudesk.orudesk.party.domain;

public class PersonNotFoundException extends RuntimeException {

    public PersonNotFoundException(PersonId id) {
        super("person not found: " + id.value());
    }
}
