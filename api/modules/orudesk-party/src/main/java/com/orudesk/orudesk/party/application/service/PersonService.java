package com.orudesk.orudesk.party.application.service;

import com.orudesk.orudesk.party.application.port.in.GetPersonQuery;
import com.orudesk.orudesk.party.application.port.in.PersonDetails;
import com.orudesk.orudesk.party.application.port.in.RegisterPersonCommand;
import com.orudesk.orudesk.party.application.port.in.RegisterPersonUseCase;
import com.orudesk.orudesk.party.application.port.out.LoadPersonPort;
import com.orudesk.orudesk.party.application.port.out.SavePersonPort;
import com.orudesk.orudesk.party.domain.Person;
import com.orudesk.orudesk.party.domain.PersonId;
import com.orudesk.orudesk.party.domain.PersonName;
import com.orudesk.orudesk.party.domain.PersonNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional
public class PersonService implements RegisterPersonUseCase, GetPersonQuery {

    private final SavePersonPort savePersonPort;
    private final LoadPersonPort loadPersonPort;
    private final Clock clock;

    public PersonService(SavePersonPort savePersonPort, LoadPersonPort loadPersonPort, Clock clock) {
        this.savePersonPort = savePersonPort;
        this.loadPersonPort = loadPersonPort;
        this.clock = clock;
    }

    @Override
    public UUID register(RegisterPersonCommand command) {
        Person person = Person.register(
                new PersonName(command.firstName(), command.lastName()),
                command.dateOfBirth(), command.phone(), command.email(),
                LocalDate.now(clock));
        savePersonPort.save(person);
        return person.id().value();
    }

    @Override
    @Transactional(readOnly = true)
    public PersonDetails getPerson(UUID personId) {
        PersonId id = new PersonId(personId);
        Person person = loadPersonPort.findById(id).orElseThrow(() -> new PersonNotFoundException(id));
        return new PersonDetails(
                person.id().value(),
                person.name().first(),
                person.name().last(),
                person.dateOfBirth().orElse(null),
                person.phone().orElse(null),
                person.email().orElse(null));
    }
}
