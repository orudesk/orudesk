package com.orudesk.orudesk.identity.application.port.out;

public record AccessToken(String value, long expiresInSeconds) {
}
