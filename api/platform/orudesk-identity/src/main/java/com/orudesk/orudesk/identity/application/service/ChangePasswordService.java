package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.ChangePasswordUseCase;
import com.orudesk.orudesk.identity.application.port.in.OwnPasswordChanged;
import com.orudesk.orudesk.identity.application.port.out.BreachedPasswords;
import com.orudesk.orudesk.identity.application.port.out.LoadUserPort;
import com.orudesk.orudesk.identity.application.port.out.PasswordHasher;
import com.orudesk.orudesk.identity.application.port.out.PasswordVerifier;
import com.orudesk.orudesk.identity.application.port.out.RefreshTokenStore;
import com.orudesk.orudesk.identity.application.port.out.SaveUserPort;
import com.orudesk.orudesk.identity.domain.BreachedPasswordException;
import com.orudesk.orudesk.identity.domain.User;
import com.orudesk.orudesk.identity.domain.UserId;
import com.orudesk.orudesk.identity.domain.UserNotFoundException;
import com.orudesk.orudesk.identity.domain.WrongPasswordException;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Changing one's own password (Day 69). The usual reason is "someone may know it", so every other session ends:
 * whoever knew the old password is signed out, and this browser stays in.
 */
@Service
public class ChangePasswordService implements ChangePasswordUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final PasswordVerifier passwordVerifier;
    private final PasswordHasher passwordHasher;
    private final BreachedPasswords breachedPasswords;
    private final RefreshTokenStore sessions;
    private final ApplicationEventPublisher events;

    public ChangePasswordService(LoadUserPort loadUserPort, SaveUserPort saveUserPort, PasswordVerifier passwordVerifier,
                                 PasswordHasher passwordHasher, BreachedPasswords breachedPasswords,
                                 RefreshTokenStore sessions, ApplicationEventPublisher events) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.passwordVerifier = passwordVerifier;
        this.passwordHasher = passwordHasher;
        this.breachedPasswords = breachedPasswords;
        this.sessions = sessions;
        this.events = events;
    }

    @Override
    @Transactional
    public void changePassword(UserId userId, @Nullable UUID currentSession, String currentPassword, String newPassword) {
        User user = loadUserPort.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (!passwordVerifier.matches(currentPassword, user.passwordHash())) {
            throw new WrongPasswordException();
        }
        if (breachedPasswords.isBreached(newPassword)) {
            throw new BreachedPasswordException("newPassword");
        }
        user.changePassword(passwordHasher.hash(newPassword));
        saveUserPort.save(user);
        int ended = sessions.endAllExcept(userId, currentSession);
        events.publishEvent(new OwnPasswordChanged(user.tenantId().value(), userId.value(), user.email().value(), ended));
    }
}
