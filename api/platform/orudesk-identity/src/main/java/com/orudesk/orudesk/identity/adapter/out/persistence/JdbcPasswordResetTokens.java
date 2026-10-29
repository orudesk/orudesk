package com.orudesk.orudesk.identity.adapter.out.persistence;

import com.orudesk.orudesk.identity.application.port.out.PasswordResetTokens;
import com.orudesk.orudesk.identity.domain.UserId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Plain SQL rather than JPA: a token has no behaviour worth an entity, and "use it up" must be one atomic
 * statement. The UPDATE ... WHERE used_at IS NULL ... RETURNING both checks and spends the token, so two requests
 * with the same link can't both succeed (the second finds used_at set and gets no row back).
 */
@Component
class JdbcPasswordResetTokens implements PasswordResetTokens {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final Duration ttl;

    JdbcPasswordResetTokens(JdbcTemplate jdbc, Clock clock,
                            @Value("${orudesk.identity.password-reset.ttl:30m}") Duration ttl) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.ttl = ttl;
    }

    @Override
    public String create(UserId userId) {
        byte[] bytes = new byte[32];   // 256 bits: guessing one is hopeless, even without the expiry
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = clock.instant();
        // Only the newest link works: an old email lying in an inbox stops being a way in
        jdbc.update("DELETE FROM iam_password_reset WHERE user_id = ? AND used_at IS NULL", userId.value());
        jdbc.update("""
                INSERT INTO iam_password_reset (token_hash, user_id, created_at, expires_at)
                VALUES (?, ?, ?, ?)
                """, hash(token), userId.value(), Timestamp.from(now), Timestamp.from(now.plus(ttl)));
        return token;
    }

    @Override
    public Optional<UserId> consume(String rawToken, Instant now) {
        List<UUID> users = jdbc.queryForList("""
                UPDATE iam_password_reset SET used_at = ?
                WHERE token_hash = ? AND used_at IS NULL AND expires_at > ?
                RETURNING user_id
                """, UUID.class, Timestamp.from(now), hash(rawToken), Timestamp.from(now));
        return users.stream().findFirst().map(UserId::new);
    }

    @Override
    public int deleteExpiredBefore(Instant cutoff) {
        return jdbc.update("DELETE FROM iam_password_reset WHERE expires_at < ?", Timestamp.from(cutoff));
    }

    private static String hash(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available in Java", e);
        }
    }
}
