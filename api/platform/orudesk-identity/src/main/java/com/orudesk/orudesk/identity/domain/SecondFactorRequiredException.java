package com.orudesk.orudesk.identity.domain;

/**
 * The password was right, and the user has two-factor sign-in on (Day 68). Not a failure: the client asks for the
 * code and sends it with this token to the second step.
 */
public class SecondFactorRequiredException extends RuntimeException {

    private final String loginToken;

    public SecondFactorRequiredException(String loginToken) {
        super("enter the code from your authenticator app, or a recovery code");
        this.loginToken = loginToken;
    }

    public String loginToken() {
        return loginToken;
    }
}
