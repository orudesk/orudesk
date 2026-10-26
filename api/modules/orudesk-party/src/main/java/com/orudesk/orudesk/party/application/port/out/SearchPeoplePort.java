package com.orudesk.orudesk.party.application.port.out;

import com.orudesk.orudesk.party.domain.Person;

import java.util.List;

public interface SearchPeoplePort {

    /**
     * People of the current tenant whose name, phone or email contains the text, sorted by
     * (last name, first name, id), starting after the given position.
     *
     * @param text  lower-case text to look for, or null for everyone
     * @param after where the previous page ended, or null for the first page
     */
    List<Person> search(String text, PersonPosition after, int limit);
}
