package com.orudesk.orudesk.identity.domain;

/** The second step's token is unknown, timed out, used, or ended by too many wrong codes (Day 68). */
public class LoginExpiredException extends RuntimeException {

    public LoginExpiredException() {
        super("this sign-in has expired; enter your email and password again");
    }
}
