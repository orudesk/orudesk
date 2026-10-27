package com.orudesk.orudesk.party.application.service;

import com.orudesk.orudesk.party.application.port.in.GetPersonQuery;
import com.orudesk.orudesk.party.application.port.in.PeopleSearch;
import com.orudesk.orudesk.party.application.port.in.PersonDetails;
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
public class PersonService implements RegisterPersonUseCase, GetPersonQuery, SearchPeopleQuery {

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
        events.publishEvent(new PersonRegistered(person.id().value(),
                person.name().first() + " " + person.name().last()));
        return person.id().value();
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
                person.email().orElse(null));
    }
}
