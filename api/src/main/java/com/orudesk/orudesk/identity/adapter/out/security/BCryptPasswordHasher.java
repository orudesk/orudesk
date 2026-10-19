package com.orudesk.orudesk.identity.adapter.out.security;

import com.orudesk.orudesk.identity.application.port.out.PasswordHasher;
import com.orudesk.orudesk.identity.domain.PasswordHash;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** Implements the PasswordHasher port with BCrypt, from the Spring Security crypto library. */
@Component
class BCryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public PasswordHash hash(String rawPassword) {
        return new PasswordHash(encoder.encode(rawPassword));
    }
}
