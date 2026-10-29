package com.orudesk.orudesk.party.application.service;

import com.orudesk.orudesk.party.application.port.out.LoadPersonPort;
import com.orudesk.orudesk.party.application.port.out.PersonPosition;
import com.orudesk.orudesk.party.application.port.out.SavePersonPort;
import com.orudesk.orudesk.party.application.port.out.SearchPeoplePort;
import com.orudesk.orudesk.party.domain.Person;
import com.orudesk.orudesk.party.domain.PersonId;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** A fake for unit tests: keeps people in a map instead of PostgreSQL. */
class InMemoryPersonStore implements SavePersonPort, LoadPersonPort, SearchPeoplePort {

    // The same order as the SQL: last name, first name, then id to break ties
    private static final Comparator<PersonPosition> ORDER = Comparator
            .comparing(PersonPosition::lastName)
            .thenComparing(PersonPosition::firstName)
            .thenComparing(PersonPosition::id);

    private final Map<PersonId, Person> people = new HashMap<>();

    @Override
    public void save(Person person) {
        people.put(person.id(), person);
    }

    /** Like the database: the stored version must match, and the saved copy moves to the next one. */
    @Override
    public long update(Person person) {
        Person stored = people.get(person.id());
        if (stored == null || stored.version() != person.version()) {
            throw new ObjectOptimisticLockingFailureException(Person.class, person.id().value());
        }
        long next = person.version() + 1;
        people.put(person.id(), Person.restore(person.id(), person.name(), person.dateOfBirth().orElse(null),
                person.phone().orElse(null), person.email().orElse(null), next));
        return next;
    }

    @Override
    public Optional<Person> findById(PersonId id) {
        return Optional.ofNullable(people.get(id));
    }

    @Override
    public List<Person> search(String text, PersonPosition after, int limit) {
        return people.values().stream()
                .filter(person -> text == null || searchText(person).contains(text))
                .filter(person -> after == null || ORDER.compare(position(person), after) > 0)
                .sorted(Comparator.comparing(InMemoryPersonStore::position, ORDER))
                .limit(limit)
                .toList();
    }

    private static PersonPosition position(Person person) {
        return new PersonPosition(person.name().last(), person.name().first(), person.id().value());
    }

    private static String searchText(Person person) {
        return (person.name().first() + " " + person.name().last() + " "
                + person.phone().orElse("") + " " + person.email().orElse("")).toLowerCase();
    }
}
