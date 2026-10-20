package com.orudesk.orudesk.identity.application.port.out;

import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.User;
import com.orudesk.orudesk.identity.domain.UserId;

import java.util.Optional;

public interface LoadUserPort {

    Optional<User> findById(UserId id);

    boolean existsByEmail(Email email);
    Optional<User> findByEmail(Email email);
}
