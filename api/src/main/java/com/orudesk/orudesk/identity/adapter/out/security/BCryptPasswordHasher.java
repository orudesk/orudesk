package com.orudesk.orudesk.identity.adapter.out.security;

import com.orudesk.orudesk.identity.application.port.out.PasswordHasher;
import com.orudesk.orudesk.identity.domain.PasswordHash;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Implements the PasswordHasher port with the PasswordEncoder bean from SecurityConfig,
 * so registration and login always use the same algorithm.
 */
@Component
class BCryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder encoder;

    BCryptPasswordHasher(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public PasswordHash hash(String rawPassword) {
        return new PasswordHash(encoder.encode(rawPassword));
    }
}
