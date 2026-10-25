package com.orudesk.orudesk.party.application.port.in;

import java.util.UUID;

/** Adds a person to the registry of the logged-in user's tenant. Verticals call this to create a patient, donor or student. */
public interface RegisterPersonUseCase {

    UUID register(RegisterPersonCommand command);
}
