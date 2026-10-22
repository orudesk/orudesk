package com.orudesk.orudesk.identity.domain;

/** The refresh token is unknown, expired, already used or revoked. */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("invalid refresh token");
    }
}
