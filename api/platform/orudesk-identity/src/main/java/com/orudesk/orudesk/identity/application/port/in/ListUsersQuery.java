package com.orudesk.orudesk.identity.application.port.in;

/** The tenant's users, a page at a time: for the admin screens. */
public interface ListUsersQuery {

    int MAX_PAGE_SIZE = 100;

    UserPage listUsers(UserSearch search);
}
