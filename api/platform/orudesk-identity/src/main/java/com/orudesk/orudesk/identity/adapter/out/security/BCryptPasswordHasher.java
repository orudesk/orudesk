package com.orudesk.orudesk.identity.adapter.out.security;

import com.orudesk.orudesk.identity.application.port.out.PasswordHasher;
import com.orudesk.orudesk.identity.application.port.out.PasswordVerifier;
import com.orudesk.orudesk.identity.domain.PasswordHash;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Implements the PasswordHasher and PasswordVerifier ports with the PasswordEncoder bean from SecurityConfig,
 * so registration and login always use the same algorithm.
 */
@Component
class BCryptPasswordHasher implements PasswordHasher, PasswordVerifier {

    private final PasswordEncoder encoder;

    BCryptPasswordHasher(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public PasswordHash hash(String rawPassword) {
        return new PasswordHash(encoder.encode(rawPassword));
    }

    @Override
    public boolean matches(String rawPassword, PasswordHash hash) {
        return encoder.matches(rawPassword, hash.value());
    }
}
