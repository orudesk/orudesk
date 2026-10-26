package com.orudesk.orudesk.identity.application.port.in;

public interface LogoutUseCase {

    void logout(String refreshToken);
}
