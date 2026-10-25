package com.orudesk.orudesk.identity.application.port.out;

import com.orudesk.orudesk.identity.domain.User;

public interface SaveUserPort {

    /** Inserts a new user or updates an existing one, including its roles. */
    void save(User user);
}
