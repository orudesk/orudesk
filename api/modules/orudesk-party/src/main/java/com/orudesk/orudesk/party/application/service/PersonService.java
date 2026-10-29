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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
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

        List<String> changedFields = changedFields(current, changed);
        if (changedFields.isEmpty()) {
            return toDetails(current);   // nothing to save, nothing to audit, and the version stays
        }
        long newVersion = savePersonPort.update(changed);
        events.publishEvent(new PersonEdited(id.value(), fullName(changed), changedFields));
        return toDetails(Person.restore(changed.id(), changed.name(), changed.dateOfBirth().orElse(null),
                changed.phone().orElse(null), changed.email().orElse(null), newVersion));
    }

    private static List<String> changedFields(Person before, Person after) {
        List<String> fields = new ArrayList<>();
        if (!before.name().first().equals(after.name().first())) fields.add("firstName");
        if (!before.name().last().equals(after.name().last())) fields.add("lastName");
        if (!before.dateOfBirth().equals(after.dateOfBirth())) fields.add("dateOfBirth");
        if (!before.phone().equals(after.phone())) fields.add("phone");
        if (!before.email().equals(after.email())) fields.add("email");
        return fields;
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
