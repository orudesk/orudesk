package com.orudesk.orudesk.identity.application.port.in;

/** Finishes a login that asked for a second factor (Day 68). */
public interface SecondFactorLoginUseCase {

    AuthTokens completeLogin(SecondFactorCommand command);
}
