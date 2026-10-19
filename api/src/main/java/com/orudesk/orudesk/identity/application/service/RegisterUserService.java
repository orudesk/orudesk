package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.RegisterUserCommand;
import com.orudesk.orudesk.identity.application.port.in.RegisterUserUseCase;
import com.orudesk.orudesk.identity.application.port.out.LoadUserPort;
import com.orudesk.orudesk.identity.application.port.out.PasswordHasher;
import com.orudesk.orudesk.identity.application.port.out.SaveUserPort;
import com.orudesk.orudesk.identity.domain.EmailAlreadyRegisteredException;
import com.orudesk.orudesk.identity.domain.User;
import com.orudesk.orudesk.identity.domain.UserId;

public class RegisterUserService implements RegisterUserUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final PasswordHasher passwordHasher;

    public RegisterUserService(LoadUserPort loadUserPort, SaveUserPort saveUserPort, PasswordHasher passwordHasher) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public UserId register(RegisterUserCommand command) {
        if (loadUserPort.existsByEmail(command.email())) {
            throw new EmailAlreadyRegisteredException(command.email());
        }
        User user = User.register(command.tenantId(), command.email(), passwordHasher.hash(command.rawPassword()));
        saveUserPort.save(user);
        return user.id();
    }
}
