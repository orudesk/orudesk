package com.orudesk.orudesk.identity.application.port.out;

import com.orudesk.orudesk.identity.domain.PasswordHash;

public interface PasswordVerifier {

    boolean matches(String rawPassword, PasswordHash hash);
}
