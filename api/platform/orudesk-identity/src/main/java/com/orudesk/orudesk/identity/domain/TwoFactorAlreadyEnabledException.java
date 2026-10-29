package com.orudesk.orudesk.identity.domain;

/** Day 67. */
public class TwoFactorAlreadyEnabledException extends RuntimeException {

    public TwoFactorAlreadyEnabledException() {
        super("two-factor sign-in is already on; turn it off first to set it up again");
    }
}
