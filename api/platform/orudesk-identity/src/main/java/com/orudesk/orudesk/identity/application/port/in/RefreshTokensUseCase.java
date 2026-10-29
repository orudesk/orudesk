package com.orudesk.orudesk.identity.application.port.in;

public interface RefreshTokensUseCase {

    /** Swaps a valid refresh token for a new pair. The old refresh token stops working; the session goes on. */
    AuthTokens refresh(String refreshToken, ClientInfo client);

    default AuthTokens refresh(String refreshToken) {
        return refresh(refreshToken, ClientInfo.unknown());
    }
}
