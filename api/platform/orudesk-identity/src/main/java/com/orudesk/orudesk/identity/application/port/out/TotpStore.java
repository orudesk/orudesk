package com.orudesk.orudesk.identity.application.port.out;

import com.orudesk.orudesk.identity.domain.UserId;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** A user's authenticator app secret and recovery codes (Day 67). */
public interface TotpStore {

    /** @param confirmedAt null while set up but not yet proven with a code */
    record StoredTotp(String sealedSecret, @Nullable Instant confirmedAt, long lastUsedStep) {
        public boolean confirmed() {
            return confirmedAt != null;
        }
    }

    Optional<StoredTotp> find(UserId userId);

    /** Replaces an unconfirmed setup, if any. */
    void savePending(UserId userId, String sealedSecret, Instant now);

    void confirm(UserId userId, long step, Instant now);

    /** Records a code's step as used, only if it's newer than the last one: false when it was already used. */
    boolean useStep(UserId userId, long step);

    /** Turns two-factor sign-in off: the secret and the recovery codes go. */
    void delete(UserId userId);

    void replaceRecoveryCodes(UserId userId, List<String> hashes);

    /** Spends a recovery code: false when there's no such unused code. */
    boolean useRecoveryCode(UserId userId, String hash, Instant now);

    int recoveryCodesLeft(UserId userId);
}
