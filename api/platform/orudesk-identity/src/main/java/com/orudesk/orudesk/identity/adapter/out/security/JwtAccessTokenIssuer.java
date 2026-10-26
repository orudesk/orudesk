package com.orudesk.orudesk.identity.adapter.out.security;

import com.orudesk.orudesk.identity.application.port.out.AccessToken;
import com.orudesk.orudesk.identity.application.port.out.AccessTokenIssuer;
import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Creates signed JWT access tokens that carry the user's id, tenant and roles. */
@Component
class JwtAccessTokenIssuer implements AccessTokenIssuer {

    static final String ISSUER = "orudesk";

    private final JwtEncoder encoder;
    private final Duration ttl;

    JwtAccessTokenIssuer(JwtEncoder encoder,
                         @Value("${orudesk.security.jwt.access-token-ttl}") Duration ttl) {
        this.encoder = encoder;
        this.ttl = ttl;
    }

    @Override
    public AccessToken issue(User user) {
        Instant now = Instant.now();
        List<String> roles = user.roles().stream().map(RoleCode::value).sorted().toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.id().value().toString())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim("email", user.email().value())
                .claim("tenant_id", user.tenantId().value().toString())
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(token, ttl.toSeconds());
    }
}
