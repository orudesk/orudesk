package com.orudesk.orudesk.party.adapter.out.persistence;

import com.orudesk.orudesk.party.application.port.out.LoadPersonPort;
import com.orudesk.orudesk.party.application.port.out.PersonPosition;
import com.orudesk.orudesk.party.application.port.out.SavePersonPort;
import com.orudesk.orudesk.party.application.port.out.SearchPeoplePort;
import com.orudesk.orudesk.party.domain.Person;
import com.orudesk.orudesk.party.domain.PersonId;
import com.orudesk.orudesk.party.domain.PersonName;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Implements the party ports with JPA, translating between Person and PersonJpaEntity. */
@Component
class PersonPersistenceAdapter implements SavePersonPort, LoadPersonPort, SearchPeoplePort {

    private final PersonJpaRepository people;
    private final EntityManager entityManager;

    PersonPersistenceAdapter(PersonJpaRepository people, EntityManager entityManager) {
        this.people = people;
        this.entityManager = entityManager;
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
    @Transactional
    public long update(Person person) {
        PersonJpaEntity entity = people.findById(person.id().value())
                .orElseThrow(() -> new IllegalStateException("updating a person that isn't there: " + person.id().value()));
        // The row in this transaction must be the version the change was made on. (Usually it was loaded a moment
        // ago by the same service call, so this only fails if it changed in between.)
        if (entity.getVersion() != person.version()) {
            throw new ObjectOptimisticLockingFailureException(PersonJpaEntity.class, person.id().value());
        }
        entity.changeDetails(
                person.name().first(),
                person.name().last(),
                person.dateOfBirth().orElse(null),
                person.phone().orElse(null),
                person.email().orElse(null));
        // Flush now, not at commit: the UPDATE ... WHERE version = ? runs here, so a lost race throws inside this
        // call, and the version read below is the new one.
        people.flush();
        return entity.getVersion();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Person> findById(PersonId id) {
        return people.findById(id.value()).map(PersonPersistenceAdapter::toDomain);
    }

    /**
     * HQL, not SQL: Hibernate adds the tenant filter to HQL queries, but never to native SQL.
     * The query is assembled from fixed pieces only; the user's text always travels as a bound parameter.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Person> search(String text, PersonPosition after, int limit) {
        StringBuilder hql = new StringBuilder("from PersonJpaEntity p where 1 = 1");
        if (text != null) {
            hql.append(" and p.searchText like :pattern escape '\\'");
        }
        if (after != null) {
            // A row-value comparison: "comes after this (last, first, id) in the sort order"
            hql.append(" and (p.lastName, p.firstName, p.id) > (:lastName, :firstName, :id)");
        }
        hql.append(" order by p.lastName, p.firstName, p.id");

        TypedQuery<PersonJpaEntity> query = entityManager.createQuery(hql.toString(), PersonJpaEntity.class);
        if (text != null) {
            query.setParameter("pattern", "%" + escapeLike(text) + "%");
        }
        if (after != null) {
            query.setParameter("lastName", after.lastName());
            query.setParameter("firstName", after.firstName());
            query.setParameter("id", after.id());
        }
        return query.setMaxResults(limit).getResultList().stream()
                .map(PersonPersistenceAdapter::toDomain)
                .toList();
    }

    /** "50%" must find "50%", not "50 and anything after". */
    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static Person toDomain(PersonJpaEntity entity) {
        return Person.restore(
                new PersonId(entity.getId()),
                new PersonName(entity.getFirstName(), entity.getLastName()),
                entity.getDateOfBirth(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getVersion());
    }
}
