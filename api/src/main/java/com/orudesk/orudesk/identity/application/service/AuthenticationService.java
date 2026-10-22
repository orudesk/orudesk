package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.AuthTokens;
import com.orudesk.orudesk.identity.application.port.in.LoginCommand;
import com.orudesk.orudesk.identity.application.port.in.LoginUseCase;
import com.orudesk.orudesk.identity.application.port.in.LogoutUseCase;
import com.orudesk.orudesk.identity.application.port.in.RefreshTokensUseCase;
import com.orudesk.orudesk.identity.application.port.out.AccessToken;
import com.orudesk.orudesk.identity.application.port.out.AccessTokenIssuer;
import com.orudesk.orudesk.identity.application.port.out.LoadUserPort;
import com.orudesk.orudesk.identity.application.port.out.PasswordVerifier;
import com.orudesk.orudesk.identity.application.port.out.RefreshTokenStore;
import com.orudesk.orudesk.identity.domain.InvalidCredentialsException;
import com.orudesk.orudesk.identity.domain.InvalidRefreshTokenException;
import com.orudesk.orudesk.identity.domain.User;
import com.orudesk.orudesk.identity.domain.UserId;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService implements LoginUseCase, RefreshTokensUseCase, LogoutUseCase {

    private final LoadUserPort loadUserPort;
    private final PasswordVerifier passwordVerifier;
    private final AccessTokenIssuer accessTokenIssuer;
    private final RefreshTokenStore refreshTokenStore;

    public AuthenticationService(LoadUserPort loadUserPort, PasswordVerifier passwordVerifier,
                                 AccessTokenIssuer accessTokenIssuer, RefreshTokenStore refreshTokenStore) {
        this.loadUserPort = loadUserPort;
        this.passwordVerifier = passwordVerifier;
        this.accessTokenIssuer = accessTokenIssuer;
        this.refreshTokenStore = refreshTokenStore;
    }

    @Override
    public AuthTokens login(LoginCommand command) {
        User user = loadUserPort.findByEmail(command.email())
                .filter(User::isEnabled)
                .filter(candidate -> passwordVerifier.matches(command.rawPassword(), candidate.passwordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return issueTokens(user);
    }

    @Override
    public AuthTokens refresh(String refreshToken) {
        UserId userId = refreshTokenStore.consume(refreshToken).orElseThrow(InvalidRefreshTokenException::new);
        User user = loadUserPort.findById(userId)
                .filter(User::isEnabled)
                .orElseThrow(InvalidRefreshTokenException::new);
        return issueTokens(user);
    }

    @Override
    public void logout(String refreshToken) {
        refreshTokenStore.revoke(refreshToken);
    }

    private AuthTokens issueTokens(User user) {
        AccessToken accessToken = accessTokenIssuer.issue(user);
        String refreshToken = refreshTokenStore.create(user.id());
        return new AuthTokens(accessToken.value(), accessToken.expiresInSeconds(), refreshToken);
    }
}
