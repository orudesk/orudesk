package com.orudesk.orudesk.identity.application.port.in;

import com.orudesk.orudesk.identity.domain.UserId;

public interface RegisterUserUseCase {

    /** Registers a new user with the USER role and returns the new user's id. */
    UserId register(RegisterUserCommand command);
}
