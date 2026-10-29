package com.orudesk.orudesk.identity.adapter.in.security;

import com.orudesk.orudesk.identity.application.port.out.BlockedUsers;
import com.orudesk.orudesk.identity.domain.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Refuses the access token of a user who was disabled after it was issued (Day 48). The shared SecurityConfig
 * runs every OAuth2TokenValidator bean after its own checks (signature, expiry, issuer), so identity adds this
 * rule without shared knowing about users.
 *
 * If Redis can't be asked, the token is let through ("fail open"), and the log says so. The other choice, refusing
 * every request while Redis is down, would turn a cache outage into a full outage; the cost of failing open is the
 * gap this closes (a disabled user's token working until it expires), and only while Redis is down.
 */
@Component
class BlockedUserTokenValidator implements OAuth2TokenValidator<Jwt> {

    private static final Logger log = LoggerFactory.getLogger(BlockedUserTokenValidator.class);
    private static final OAuth2Error BLOCKED = new OAuth2Error("invalid_token", "this login has been disabled", null);

    private final BlockedUsers blockedUsers;

    BlockedUserTokenValidator(BlockedUsers blockedUsers) {
        this.blockedUsers = blockedUsers;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        try {
            if (blockedUsers.isBlocked(new UserId(UUID.fromString(token.getSubject())))) {
                return OAuth2TokenValidatorResult.failure(BLOCKED);
            }
        } catch (RuntimeException e) {
            log.warn("Could not check whether user {} is blocked, letting the token through: {}",
                    token.getSubject(), e.getMessage());
        }
        return OAuth2TokenValidatorResult.success();
    }
}
