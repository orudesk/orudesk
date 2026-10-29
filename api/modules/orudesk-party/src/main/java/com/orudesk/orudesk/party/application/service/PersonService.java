package com.orudesk.orudesk.party.application.service;

import com.orudesk.orudesk.party.application.port.in.EditPersonCommand;
import com.orudesk.orudesk.party.application.port.in.EditPersonUseCase;
import com.orudesk.orudesk.party.application.port.in.GetPersonQuery;
import com.orudesk.orudesk.party.application.port.in.PeopleSearch;
import com.orudesk.orudesk.party.application.port.in.PersonDetails;
import com.orudesk.orudesk.party.application.port.in.PersonEdited;
import com.orudesk.orudesk.party.application.port.in.PersonPage;
import com.orudesk.orudesk.party.application.port.in.PersonRegistered;
import com.orudesk.orudesk.party.application.port.in.RegisterPersonCommand;
import com.orudesk.orudesk.party.application.port.in.RegisterPersonUseCase;
import com.orudesk.orudesk.party.application.port.in.SearchPeopleQuery;
import com.orudesk.orudesk.party.application.port.out.LoadPersonPort;
import com.orudesk.orudesk.party.application.port.out.PersonPosition;
import com.orudesk.orudesk.party.application.port.out.SavePersonPort;
import com.orudesk.orudesk.party.application.port.out.SearchPeoplePort;
import com.orudesk.orudesk.party.domain.Person;
import com.orudesk.orudesk.party.domain.PersonId;
import com.orudesk.orudesk.party.domain.PersonName;
import com.orudesk.orudesk.party.domain.PersonNotFoundException;
import com.orudesk.orudesk.shared.EditConflictException;
import com.orudesk.orudesk.shared.FieldChange;
import com.orudesk.orudesk.shared.FieldChanges;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class PersonService implements RegisterPersonUseCase, EditPersonUseCase, GetPersonQuery, SearchPeopleQuery {

    private final SavePersonPort savePersonPort;
    private final LoadPersonPort loadPersonPort;
    private final SearchPeoplePort searchPeoplePort;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public PersonService(SavePersonPort savePersonPort, LoadPersonPort loadPersonPort,
                         SearchPeoplePort searchPeoplePort, Clock clock, ApplicationEventPublisher events) {
        this.events = events;
        this.savePersonPort = savePersonPort;
        this.loadPersonPort = loadPersonPort;
        this.searchPeoplePort = searchPeoplePort;
        this.clock = clock;
    }

    @Override
    public UUID register(RegisterPersonCommand command) {
        Person person = Person.register(
                new PersonName(command.firstName(), command.lastName()),
                command.dateOfBirth(), command.phone(), command.email(),
                LocalDate.now(clock));
        savePersonPort.save(person);
        events.publishEvent(new PersonRegistered(person.id().value(), fullName(person)));
        return person.id().value();
    }

    @Override
    public PersonDetails edit(EditPersonCommand command) {
        PersonId id = new PersonId(command.personId());
        Person current = loadPersonPort.findById(id).orElseThrow(() -> new PersonNotFoundException(id));
        // The usual conflict: the form was opened minutes ago and someone saved in between. Caught here, by
        // comparing versions. (Two saves racing within milliseconds are caught by the database: see update.)
        if (current.version() != command.version()) {
            throw new EditConflictException("person", id.value(), command.version(), current.version());
        }
        Person changed = current.changeDetails(
                new PersonName(command.firstName(), command.lastName()),
                command.dateOfBirth(), command.phone(), command.email(),
                LocalDate.now(clock));

        List<FieldChange> changes = new FieldChanges()
                .compare("firstName", current.name().first(), changed.name().first())
                .compare("lastName", current.name().last(), changed.name().last())
                .compare("dateOfBirth", current.dateOfBirth(), changed.dateOfBirth())
                .compare("phone", current.phone(), changed.phone())
                .compare("email", current.email(), changed.email())
                .list();
        if (changes.isEmpty()) {
            return toDetails(current);   // nothing to save, nothing to audit, and the version stays
        }
        long newVersion = savePersonPort.update(changed);
        events.publishEvent(new PersonEdited(id.value(), fullName(changed), changes));
        return toDetails(Person.restore(changed.id(), changed.name(), changed.dateOfBirth().orElse(null),
                changed.phone().orElse(null), changed.email().orElse(null), newVersion));
    }

    private static String fullName(Person person) {
        return person.name().first() + " " + person.name().last();
    }

    @Override
    @Transactional(readOnly = true)
    public PersonDetails getPerson(UUID personId) {
        PersonId id = new PersonId(personId);
        Person person = loadPersonPort.findById(id).orElseThrow(() -> new PersonNotFoundException(id));
        return toDetails(person);
    }

    @Override
    @Transactional(readOnly = true)
    public PersonPage search(PeopleSearch search) {
        String text = search.text() == null ? null : search.text().toLowerCase(Locale.ROOT);
        PersonPosition after = search.after() == null ? null : PersonCursor.decode(search.after());

        // Ask for one more than a page: if it comes back, there is a next page.
        List<Person> found = searchPeoplePort.search(text, after, search.size() + 1);
        boolean hasMore = found.size() > search.size();
        List<Person> page = hasMore ? found.subList(0, search.size()) : found;

        String nextCursor = hasMore ? PersonCursor.encode(page.getLast()) : null;
        return new PersonPage(page.stream().map(PersonService::toDetails).toList(), nextCursor);
    }

    private static PersonDetails toDetails(Person person) {
        return new PersonDetails(
                person.id().value(),
                person.name().first(),
                person.name().last(),
                person.dateOfBirth().orElse(null),
                person.phone().orElse(null),
                person.email().orElse(null),
                person.version());
    }
}
