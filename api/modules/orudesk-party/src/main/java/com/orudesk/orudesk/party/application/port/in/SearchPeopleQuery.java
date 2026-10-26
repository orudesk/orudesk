package com.orudesk.orudesk.party.application.port.in;

/** Finds people in the logged-in user's tenant, one page at a time, sorted by last name, first name. */
public interface SearchPeopleQuery {

    int MAX_PAGE_SIZE = 100;

    PersonPage search(PeopleSearch search);
}
