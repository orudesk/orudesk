package com.orudesk.orudesk.party.application.port.out;

import com.orudesk.orudesk.party.domain.Person;
import com.orudesk.orudesk.party.domain.PersonId;

import java.util.Optional;

public interface LoadPersonPort {

    /** Only finds people of the current tenant. */
    Optional<Person> findById(PersonId id);
}
