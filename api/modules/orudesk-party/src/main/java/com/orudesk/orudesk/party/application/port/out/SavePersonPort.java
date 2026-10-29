package com.orudesk.orudesk.party.application.port.out;

import com.orudesk.orudesk.party.domain.Person;

public interface SavePersonPort {

    /** Adds a new person. */
    void save(Person person);

    /**
     * Writes an existing person's new details, if the stored row is still at person.version(); throws
     * OptimisticLockingFailureException if another save got there first. Returns the version after the save.
     */
    long update(Person person);
}
