package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.PasswordChanged;
import com.orudesk.orudesk.identity.application.port.in.PasswordResetRequested;
import com.orudesk.orudesk.identity.application.port.in.PasswordResetUseCase;
import com.orudesk.orudesk.identity.application.port.out.BreachedPasswords;
import com.orudesk.orudesk.identity.application.port.out.LoadUserPort;
import com.orudesk.orudesk.identity.application.port.out.LoginAttemptLimiter;
import com.orudesk.orudesk.identity.application.port.out.PasswordHasher;
import com.orudesk.orudesk.identity.application.port.out.PasswordResetTokens;
import com.orudesk.orudesk.identity.application.port.out.RefreshTokenStore;
import com.orudesk.orudesk.identity.application.port.out.SaveUserPort;
import com.orudesk.orudesk.identity.domain.BreachedPasswordException;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.InvalidPasswordResetTokenException;
import com.orudesk.orudesk.identity.domain.User;
import com.orudesk.orudesk.identity.domain.UserId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

/**
 * Password reset by email (Day 53). The request answers the same whether or not the email has an account, and
 * the work that differs (a token, an email) happens later, in the outbox listener, so the response time doesn't
 * tell either.
 */
@Service
@Transactional
public class PasswordResetService implements PasswordResetUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final PasswordHasher passwordHasher;
    private final PasswordResetTokens resetTokens;
    private final RefreshTokenStore refreshTokens;
    private final LoginAttemptLimiter loginAttempts;
    private final BreachedPasswords breachedPasswords;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public PasswordResetService(LoadUserPort loadUserPort, SaveUserPort saveUserPort, PasswordHasher passwordHasher,
                                PasswordResetTokens resetTokens, RefreshTokenStore refreshTokens,
                                LoginAttemptLimiter loginAttempts, BreachedPasswords breachedPasswords,
                                ApplicationEventPublisher events, Clock clock) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.passwordHasher = passwordHasher;
        this.resetTokens = resetTokens;
        this.refreshTokens = refreshTokens;
        this.loginAttempts = loginAttempts;
        this.breachedPasswords = breachedPasswords;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public void requestReset(Email email) {
        loadUserPort.findByEmail(email)
                .filter(User::isEnabled)   // a disabled user must ask an admin, not reset their way back in
                .ifPresent(user -> events.publishEvent(new PasswordResetRequested(user.id().value(), user.email().value())));
    }

    @Override
    public Optional<String> issueToken(UserId userId) {
        return loadUserPort.findById(userId)
                .filter(User::isEnabled)
                .map(user -> resetTokens.create(user.id()));
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        // Before the token is spent, so the user can try another password with the same link
        if (breachedPasswords.isBreached(newPassword)) {
            throw new BreachedPasswordException("newPassword");
        }
        // One statement checks and spends the token; if anything below fails, the transaction gives it back
        UserId userId = resetTokens.consume(token, clock.instant()).orElseThrow(InvalidPasswordResetTokenException::new);
        User user = loadUserPort.findById(userId)
                .filter(User::isEnabled)
                .orElseThrow(InvalidPasswordResetTokenException::new);
        user.changePassword(passwordHasher.hash(newPassword));
        saveUserPort.save(user);
        // Whoever knew the old password may have a session: end them all. Their access tokens die within one
        // access-token lifetime (15 minutes); the next refresh fails at once.
        refreshTokens.revokeAll(userId);
        // Someone locked out by guessing (maybe the user themself) can log in with the new password straight away
        loginAttempts.succeeded(user.email());
        events.publishEvent(new PasswordChanged(user.tenantId().value(), userId.value(), user.email().value()));
    }
}
