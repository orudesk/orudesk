package com.orudesk.orudesk.identity.adapter.in.security;

import com.orudesk.orudesk.identity.application.port.out.RefreshTokenStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Refuses an access token whose session has ended (Day 66): after logout, "sign out" on another device, or a
 * password reset, the access token stops working at once instead of living out its 15 minutes. Tokens issued
 * before Day 66 name no session and pass.
 *
 * Like {@link BlockedUserTokenValidator}, it lets the token through when Redis can't be asked (fail open): the
 * session store being down already stops every refresh, so the gap is at most one access token lifetime.
 */
@Component
class EndedSessionTokenValidator implements OAuth2TokenValidator<Jwt> {

    private static final Logger log = LoggerFactory.getLogger(EndedSessionTokenValidator.class);
    private static final OAuth2Error ENDED = new OAuth2Error("invalid_token", "this session has ended", null);

    private final RefreshTokenStore sessions;

    EndedSessionTokenValidator(RefreshTokenStore sessions) {
        this.sessions = sessions;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String sid = token.getClaimAsString("sid");
        if (sid == null) {
            return OAuth2TokenValidatorResult.success();
        }
        try {
            if (!sessions.isActive(UUID.fromString(sid))) {
                return OAuth2TokenValidatorResult.failure(ENDED);
            }
        } catch (RuntimeException e) {
            log.warn("Could not check session {} of user {}, letting the token through: {}", sid, token.getSubject(),
                    e.getMessage());
        }
        return OAuth2TokenValidatorResult.success();
    }
}
