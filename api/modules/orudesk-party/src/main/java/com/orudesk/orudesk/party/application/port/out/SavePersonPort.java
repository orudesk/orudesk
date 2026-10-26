package com.orudesk.orudesk.party.application.port.out;

import com.orudesk.orudesk.party.domain.Person;

public interface SavePersonPort {

    void save(Person person);
}
