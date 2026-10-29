package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.RecoveryCodeUsed;
import com.orudesk.orudesk.identity.application.port.out.SecretBox;
import com.orudesk.orudesk.identity.application.port.out.TotpStore;
import com.orudesk.orudesk.identity.domain.RecoveryCodes;
import com.orudesk.orudesk.identity.domain.Totp;
import com.orudesk.orudesk.identity.domain.TwoFactorNotAvailableException;
import com.orudesk.orudesk.identity.domain.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

/** Checks a login's second factor (Day 68): a code from the app, or a recovery code. */
@Component
class SecondFactor {

    private final TotpStore store;
    private final SecretBox secretBox;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    SecondFactor(TotpStore store, SecretBox secretBox, ApplicationEventPublisher events, Clock clock) {
        this.store = store;
        this.secretBox = secretBox;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public boolean requiredFor(User user) {
        return store.find(user.id()).filter(TotpStore.StoredTotp::confirmed).isPresent();
    }

    /** Spends the code: true once, false if it's wrong or was already used. */
    @Transactional
    public boolean check(User user, String code) {
        Optional<TotpStore.StoredTotp> totp = store.find(user.id()).filter(TotpStore.StoredTotp::confirmed);
        if (totp.isEmpty()) {
            return false;
        }
        Instant now = clock.instant();
        if (RecoveryCodes.looksLikeOne(code)) {
            if (!store.useRecoveryCode(user.id(), RecoveryCodes.hash(code), now)) {
                return false;
            }
            events.publishEvent(new RecoveryCodeUsed(user.tenantId().value(), user.id().value(), user.email().value(),
                    store.recoveryCodesLeft(user.id())));
            return true;
        }
        // Fail closed: if the key went missing the secret can't be read, and the password alone must not be enough
        if (!secretBox.available()) {
            throw new TwoFactorNotAvailableException();
        }
        OptionalLong step = Totp.verify(secretBox.open(totp.get().sealedSecret()), code, now, totp.get().lastUsedStep());
        // useStep is the real check against replay: two requests with one code can both pass verify, only one wins
        return step.isPresent() && store.useStep(user.id(), step.getAsLong());
    }
}
