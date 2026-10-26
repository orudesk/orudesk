package com.orudesk.orudesk.identity.application.port.out;

import com.orudesk.orudesk.identity.domain.PasswordHash;

public interface PasswordHasher {

    PasswordHash hash(String rawPassword);
}
