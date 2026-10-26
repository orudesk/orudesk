package com.orudesk.orudesk.party.application.port.in;

import java.util.UUID;

public interface GetPersonQuery {

    /** @throws com.orudesk.orudesk.party.domain.PersonNotFoundException if there's no such person in your tenant */
    PersonDetails getPerson(UUID personId);
}
