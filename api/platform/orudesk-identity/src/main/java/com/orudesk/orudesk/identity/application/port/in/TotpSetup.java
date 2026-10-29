package com.orudesk.orudesk.identity.application.port.in;

/** @param otpauthUri what the QR code holds: otpauth://totp/Orudesk:asha@example.org?secret=...&issuer=Orudesk */
public record TotpSetup(String secret, String otpauthUri) {
}
