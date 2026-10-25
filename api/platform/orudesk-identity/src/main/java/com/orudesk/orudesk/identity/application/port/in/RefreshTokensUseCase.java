package com.orudesk.orudesk.identity.application.port.in;

public interface RefreshTokensUseCase {

    /** Swaps a valid refresh token for a new pair. The old refresh token stops working. */
    AuthTokens refresh(String refreshToken);
}
