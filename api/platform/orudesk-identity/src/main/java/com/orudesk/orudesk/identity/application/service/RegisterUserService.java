package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.RegisterUserCommand;
import com.orudesk.orudesk.identity.application.port.in.RegisterUserUseCase;
import com.orudesk.orudesk.identity.application.port.in.UserRegistered;
import com.orudesk.orudesk.identity.application.port.out.BreachedPasswords;
import com.orudesk.orudesk.identity.application.port.out.LoadUserPort;
import com.orudesk.orudesk.identity.application.port.out.PasswordHasher;
import com.orudesk.orudesk.identity.application.port.out.SaveUserPort;
import com.orudesk.orudesk.identity.domain.BreachedPasswordException;
import com.orudesk.orudesk.identity.domain.EmailAlreadyRegisteredException;
import com.orudesk.orudesk.identity.domain.User;
import com.orudesk.orudesk.identity.domain.UserId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RegisterUserService implements RegisterUserUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final PasswordHasher passwordHasher;
    private final BreachedPasswords breachedPasswords;
    private final ApplicationEventPublisher events;

    public RegisterUserService(LoadUserPort loadUserPort, SaveUserPort saveUserPort, PasswordHasher passwordHasher,
                               BreachedPasswords breachedPasswords, ApplicationEventPublisher events) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.passwordHasher = passwordHasher;
        this.breachedPasswords = breachedPasswords;
        this.events = events;
    }

    @Override
    public UserId register(RegisterUserCommand command) {
        if (loadUserPort.existsByEmail(command.email())) {
            throw new EmailAlreadyRegisteredException(command.email());
        }
        if (command.checkBreaches() && breachedPasswords.isBreached(command.rawPassword())) {
            throw new BreachedPasswordException("password");
        }
        User user = User.register(command.tenantId(), command.email(), passwordHasher.hash(command.rawPassword()));
        saveUserPort.save(user);
        events.publishEvent(new UserRegistered(user.tenantId().value(), user.id().value(), user.email().value()));
        return user.id();
    }
}
