package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.TotpSetup;
import com.orudesk.orudesk.identity.application.port.in.TwoFactorDisabled;
import com.orudesk.orudesk.identity.application.port.in.TwoFactorEnabled;
import com.orudesk.orudesk.identity.application.port.in.TwoFactorStatus;
import com.orudesk.orudesk.identity.application.port.in.TwoFactorUseCase;
import com.orudesk.orudesk.identity.application.port.out.LoadUserPort;
import com.orudesk.orudesk.identity.application.port.out.PasswordVerifier;
import com.orudesk.orudesk.identity.application.port.out.SecretBox;
import com.orudesk.orudesk.identity.application.port.out.TotpStore;
import com.orudesk.orudesk.identity.domain.Base32;
import com.orudesk.orudesk.identity.domain.RecoveryCodes;
import com.orudesk.orudesk.identity.domain.Totp;
import com.orudesk.orudesk.identity.domain.TwoFactorAlreadyEnabledException;
import com.orudesk.orudesk.identity.domain.TwoFactorNotAvailableException;
import com.orudesk.orudesk.identity.domain.User;
import com.orudesk.orudesk.identity.domain.UserId;
import com.orudesk.orudesk.identity.domain.UserNotFoundException;
import com.orudesk.orudesk.identity.domain.WrongPasswordException;
import com.orudesk.orudesk.identity.domain.WrongTwoFactorCodeException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.OptionalLong;

/**
 * Setting up two-factor sign-in (Day 67). Two steps, so a user can't lock themselves out with an app that was never
 * set up right: the secret is stored unconfirmed, and only a correct code from the app turns it on.
 */
@Service
public class TwoFactorService implements TwoFactorUseCase {

    static final String ISSUER = "Orudesk";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final LoadUserPort loadUserPort;
    private final PasswordVerifier passwordVerifier;
    private final TotpStore store;
    private final SecretBox secretBox;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public TwoFactorService(LoadUserPort loadUserPort, PasswordVerifier passwordVerifier, TotpStore store,
                            SecretBox secretBox, ApplicationEventPublisher events, Clock clock) {
        this.loadUserPort = loadUserPort;
        this.passwordVerifier = passwordVerifier;
        this.store = store;
        this.secretBox = secretBox;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public TwoFactorStatus status(UserId userId) {
        return store.find(userId).filter(TotpStore.StoredTotp::confirmed)
                .map(t -> new TwoFactorStatus(secretBox.available(), true, t.confirmedAt(), store.recoveryCodesLeft(userId)))
                .orElse(new TwoFactorStatus(secretBox.available(), false, null, 0));
    }

    @Override
    @Transactional
    public TotpSetup beginSetup(UserId userId, String currentPassword) {
        if (!secretBox.available()) {
            throw new TwoFactorNotAvailableException();
        }
        User user = checkPassword(userId, currentPassword);
        if (store.find(userId).map(TotpStore.StoredTotp::confirmed).orElse(false)) {
            throw new TwoFactorAlreadyEnabledException();
        }
        byte[] secret = new byte[20];   // 160 bits, the size RFC 4226 recommends for HMAC-SHA1
        RANDOM.nextBytes(secret);
        store.savePending(userId, secretBox.seal(secret), clock.instant());
        String text = Base32.encode(secret);
        String label = URLEncoder.encode(ISSUER + ":" + user.email().value(), StandardCharsets.UTF_8).replace("+", "%20");
        return new TotpSetup(text, "otpauth://totp/" + label + "?secret=" + text + "&issuer=" + ISSUER
                + "&algorithm=SHA1&digits=" + Totp.DIGITS + "&period=" + Totp.STEP_SECONDS);
    }

    @Override
    @Transactional
    public List<String> confirmSetup(UserId userId, String code) {
        TotpStore.StoredTotp pending = store.find(userId).orElseThrow(WrongTwoFactorCodeException::new);
        if (pending.confirmed()) {
            throw new TwoFactorAlreadyEnabledException();
        }
        Instant now = clock.instant();
        OptionalLong step = Totp.verify(secretBox.open(pending.sealedSecret()), code, now, pending.lastUsedStep());
        if (step.isEmpty()) {
            throw new WrongTwoFactorCodeException();
        }
        store.confirm(userId, step.getAsLong(), now);
        List<String> codes = newCodes(userId);
        User user = user(userId);
        events.publishEvent(new TwoFactorEnabled(user.tenantId().value(), userId.value(), user.email().value()));
        return codes;
    }

    @Override
    @Transactional
    public List<String> newRecoveryCodes(UserId userId, String currentPassword) {
        checkPassword(userId, currentPassword);
        if (!store.find(userId).map(TotpStore.StoredTotp::confirmed).orElse(false)) {
            throw new IllegalArgumentException("two-factor sign-in is off: there are no recovery codes to replace");
        }
        return newCodes(userId);
    }

    @Override
    @Transactional
    public void disable(UserId userId, String currentPassword) {
        User user = checkPassword(userId, currentPassword);
        boolean wasOn = store.find(userId).map(TotpStore.StoredTotp::confirmed).orElse(false);
        store.delete(userId);
        if (wasOn) {
            events.publishEvent(new TwoFactorDisabled(user.tenantId().value(), userId.value(), user.email().value()));
        }
    }

    private List<String> newCodes(UserId userId) {
        List<String> codes = RecoveryCodes.generate();
        store.replaceRecoveryCodes(userId, codes.stream().map(RecoveryCodes::hash).toList());
        return codes;
    }

    private User checkPassword(UserId userId, String currentPassword) {
        User user = user(userId);
        if (!passwordVerifier.matches(currentPassword, user.passwordHash())) {
            throw new WrongPasswordException();
        }
        return user;
    }

    private User user(UserId userId) {
        return loadUserPort.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }
}
