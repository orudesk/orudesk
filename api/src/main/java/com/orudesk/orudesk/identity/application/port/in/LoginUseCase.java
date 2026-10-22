package com.orudesk.orudesk.identity.application.port.in;

public interface LoginUseCase {

    AuthTokens login(LoginCommand command);
}
