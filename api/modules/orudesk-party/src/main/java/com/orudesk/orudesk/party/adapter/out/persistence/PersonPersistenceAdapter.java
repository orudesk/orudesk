package com.orudesk.orudesk.party.adapter.out.persistence;

import com.orudesk.orudesk.party.application.port.out.LoadPersonPort;
import com.orudesk.orudesk.party.application.port.out.SavePersonPort;
import com.orudesk.orudesk.party.domain.Person;
import com.orudesk.orudesk.party.domain.PersonId;
import com.orudesk.orudesk.party.domain.PersonName;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Implements the party ports with JPA, translating between Person and PersonJpaEntity. */
@Component
class PersonPersistenceAdapter implements SavePersonPort, LoadPersonPort {

    private final PersonJpaRepository people;

    PersonPersistenceAdapter(PersonJpaRepository people) {
        this.people = people;
    }

    @Override
    @Transactional
    public void save(Person person) {
        people.save(new PersonJpaEntity(
                person.id().value(),
                person.name().first(),
                person.name().last(),
                person.dateOfBirth().orElse(null),
                person.phone().orElse(null),
                person.email().orElse(null)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Person> findById(PersonId id) {
        return people.findById(id.value()).map(entity -> Person.restore(
                new PersonId(entity.getId()),
                new PersonName(entity.getFirstName(), entity.getLastName()),
                entity.getDateOfBirth(),
                entity.getPhone(),
                entity.getEmail()));
    }
}
