package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.AuthTokens;
import com.orudesk.orudesk.identity.application.port.in.LoginCommand;
import com.orudesk.orudesk.identity.application.port.in.LoginUseCase;
import com.orudesk.orudesk.identity.application.port.in.LogoutUseCase;
import com.orudesk.orudesk.identity.application.port.in.RefreshTokensUseCase;
import com.orudesk.orudesk.identity.application.port.out.AccessToken;
import com.orudesk.orudesk.identity.application.port.out.AccessTokenIssuer;
import com.orudesk.orudesk.identity.application.port.out.LoadUserPort;
import com.orudesk.orudesk.identity.application.port.out.LoginAttemptLimiter;
import com.orudesk.orudesk.identity.application.port.out.PasswordVerifier;
import com.orudesk.orudesk.identity.application.port.out.RefreshTokenStore;
import com.orudesk.orudesk.identity.domain.InvalidCredentialsException;
import com.orudesk.orudesk.identity.domain.InvalidRefreshTokenException;
import com.orudesk.orudesk.identity.domain.TooManyLoginAttemptsException;
import com.orudesk.orudesk.identity.domain.User;
import com.orudesk.orudesk.identity.domain.UserId;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class AuthenticationService implements LoginUseCase, RefreshTokensUseCase, LogoutUseCase {

    private final LoadUserPort loadUserPort;
    private final PasswordVerifier passwordVerifier;
    private final AccessTokenIssuer accessTokenIssuer;
    private final RefreshTokenStore refreshTokenStore;
    private final LoginAttemptLimiter loginAttempts;

    public AuthenticationService(LoadUserPort loadUserPort, PasswordVerifier passwordVerifier,
                                 AccessTokenIssuer accessTokenIssuer, RefreshTokenStore refreshTokenStore,
                                 LoginAttemptLimiter loginAttempts) {
        this.loadUserPort = loadUserPort;
        this.passwordVerifier = passwordVerifier;
        this.accessTokenIssuer = accessTokenIssuer;
        this.refreshTokenStore = refreshTokenStore;
        this.loginAttempts = loginAttempts;
    }

    @Override
    public AuthTokens login(LoginCommand command) {
        // Asked first, before the password is even checked: a blocked guesser learns nothing from a right guess
        Optional<Duration> blocked = loginAttempts.blockedFor(command.email());
        if (blocked.isPresent()) {
            throw new TooManyLoginAttemptsException(blocked.get());
        }
        Optional<User> user = loadUserPort.findByEmail(command.email())
                .filter(User::isEnabled)
                .filter(candidate -> passwordVerifier.matches(command.rawPassword(), candidate.passwordHash()));
        if (user.isEmpty()) {
            loginAttempts.failed(command.email());   // unknown emails too, or the limit would reveal who has an account
            throw new InvalidCredentialsException();
        }
        loginAttempts.succeeded(command.email());
        return issueTokens(user.get());
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
