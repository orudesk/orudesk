package com.orudesk.orudesk.party.application.port.in;

/**
 * Changes a person's details. Throws EditConflictException if the person was changed after command.version()
 * was read, and PersonNotFoundException if they aren't in the tenant. Verticals call it from their own edit
 * screens (a patient's phone number lives here, not in the patient record).
 */
public interface EditPersonUseCase {

    /** Returns the person as saved, with the new version. */
    PersonDetails edit(EditPersonCommand command);
}
