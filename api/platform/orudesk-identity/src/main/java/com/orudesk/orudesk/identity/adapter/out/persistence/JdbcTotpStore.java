package com.orudesk.orudesk.identity.adapter.out.persistence;

import com.orudesk.orudesk.identity.application.port.out.TotpStore;
import com.orudesk.orudesk.identity.domain.UserId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Plain SQL, like the reset tokens (Day 53): small rows, and "use this code once" must be one atomic UPDATE whose
 * WHERE does the check, so two logins racing with the same code can't both win.
 */
@Component
class JdbcTotpStore implements TotpStore {

    private final JdbcTemplate jdbc;

    JdbcTotpStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<StoredTotp> find(UserId userId) {
        return jdbc.query("SELECT secret_sealed, confirmed_at, last_used_step FROM iam_user_totp WHERE user_id = ?",
                (rs, i) -> {
                    Timestamp confirmed = rs.getTimestamp("confirmed_at");
                    return new StoredTotp(rs.getString("secret_sealed"), confirmed == null ? null : confirmed.toInstant(),
                            rs.getLong("last_used_step"));
                }, userId.value()).stream().findFirst();
    }

    @Override
    public void savePending(UserId userId, String sealedSecret, Instant now) {
        jdbc.update("""
                INSERT INTO iam_user_totp (user_id, secret_sealed, created_at) VALUES (?, ?, ?)
                ON CONFLICT (user_id) DO UPDATE SET secret_sealed = EXCLUDED.secret_sealed,
                    created_at = EXCLUDED.created_at, last_used_step = 0
                WHERE iam_user_totp.confirmed_at IS NULL
                """, userId.value(), sealedSecret, Timestamp.from(now));
    }

    @Override
    public void confirm(UserId userId, long step, Instant now) {
        jdbc.update("UPDATE iam_user_totp SET confirmed_at = ?, last_used_step = ? WHERE user_id = ?",
                Timestamp.from(now), step, userId.value());
    }

    @Override
    public boolean useStep(UserId userId, long step) {
        return jdbc.update("UPDATE iam_user_totp SET last_used_step = ? WHERE user_id = ? AND last_used_step < ?",
                step, userId.value(), step) == 1;
    }

    @Override
    public void delete(UserId userId) {
        jdbc.update("DELETE FROM iam_recovery_code WHERE user_id = ?", userId.value());
        jdbc.update("DELETE FROM iam_user_totp WHERE user_id = ?", userId.value());
    }

    @Override
    public void replaceRecoveryCodes(UserId userId, List<String> hashes) {
        jdbc.update("DELETE FROM iam_recovery_code WHERE user_id = ?", userId.value());
        jdbc.batchUpdate("INSERT INTO iam_recovery_code (user_id, code_hash) VALUES (?, ?)",
                hashes.stream().map(h -> new Object[]{userId.value(), h}).toList());
    }

    @Override
    public boolean useRecoveryCode(UserId userId, String hash, Instant now) {
        return jdbc.update("UPDATE iam_recovery_code SET used_at = ? WHERE user_id = ? AND code_hash = ? AND used_at IS NULL",
                Timestamp.from(now), userId.value(), hash) == 1;
    }

    @Override
    public int recoveryCodesLeft(UserId userId) {
        Integer left = jdbc.queryForObject("SELECT COUNT(*) FROM iam_recovery_code WHERE user_id = ? AND used_at IS NULL",
                Integer.class, userId.value());
        return left == null ? 0 : left;
    }
}
