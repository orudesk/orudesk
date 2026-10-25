package com.orudesk.orudesk.party.application.service;

import com.orudesk.orudesk.party.application.port.out.LoadPersonPort;
import com.orudesk.orudesk.party.application.port.out.SavePersonPort;
import com.orudesk.orudesk.party.domain.Person;
import com.orudesk.orudesk.party.domain.PersonId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** A fake for unit tests: keeps people in a map instead of PostgreSQL. */
class InMemoryPersonStore implements SavePersonPort, LoadPersonPort {

    private final Map<PersonId, Person> people = new HashMap<>();

    @Override
    public void save(Person person) {
        people.put(person.id(), person);
    }

    @Override
    public Optional<Person> findById(PersonId id) {
        return Optional.ofNullable(people.get(id));
    }
}
