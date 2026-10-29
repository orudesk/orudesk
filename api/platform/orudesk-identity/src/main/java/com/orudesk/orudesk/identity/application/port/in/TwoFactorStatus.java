package com.orudesk.orudesk.identity.application.port.in;

import org.jspecify.annotations.Nullable;

import java.time.Instant;

/** @param available false when the server has no key to keep secrets with (ORUDESK_MFA_KEY) */
public record TwoFactorStatus(boolean available, boolean enabled, @Nullable Instant enabledAt, int recoveryCodesLeft) {
}
