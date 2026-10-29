package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.AuthTokens;
import com.orudesk.orudesk.identity.application.port.in.ClientInfo;
import com.orudesk.orudesk.identity.application.port.in.LoginCommand;
import com.orudesk.orudesk.identity.application.port.in.LoginUseCase;
import com.orudesk.orudesk.identity.application.port.in.LogoutUseCase;
import com.orudesk.orudesk.identity.application.port.in.RefreshTokensUseCase;
import com.orudesk.orudesk.identity.application.port.in.SecondFactorCommand;
import com.orudesk.orudesk.identity.application.port.in.SecondFactorLoginUseCase;
import com.orudesk.orudesk.identity.application.port.out.AccessToken;
import com.orudesk.orudesk.identity.application.port.out.AccessTokenIssuer;
import com.orudesk.orudesk.identity.application.port.out.LoadUserPort;
import com.orudesk.orudesk.identity.application.port.out.LoginAttemptLimiter;
import com.orudesk.orudesk.identity.application.port.out.PasswordVerifier;
import com.orudesk.orudesk.identity.application.port.out.PendingLogins;
import com.orudesk.orudesk.identity.application.port.out.RefreshGrant;
import com.orudesk.orudesk.identity.application.port.out.RefreshTokenStore;
import com.orudesk.orudesk.identity.application.port.out.SessionRef;
import com.orudesk.orudesk.identity.domain.InvalidCredentialsException;
import com.orudesk.orudesk.identity.domain.InvalidRefreshTokenException;
import com.orudesk.orudesk.identity.domain.LoginExpiredException;
import com.orudesk.orudesk.identity.domain.SecondFactorRequiredException;
import com.orudesk.orudesk.identity.domain.TooManyLoginAttemptsException;
import com.orudesk.orudesk.identity.domain.User;
import com.orudesk.orudesk.identity.domain.WrongTwoFactorCodeException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthenticationService implements LoginUseCase, SecondFactorLoginUseCase, RefreshTokensUseCase, LogoutUseCase {

    private final LoadUserPort loadUserPort;
    private final PasswordVerifier passwordVerifier;
    private final AccessTokenIssuer accessTokenIssuer;
    private final RefreshTokenStore refreshTokenStore;
    private final LoginAttemptLimiter loginAttempts;
    private final SecondFactor secondFactor;
    private final PendingLogins pendingLogins;

    public AuthenticationService(LoadUserPort loadUserPort, PasswordVerifier passwordVerifier,
                                 AccessTokenIssuer accessTokenIssuer, RefreshTokenStore refreshTokenStore,
                                 LoginAttemptLimiter loginAttempts, SecondFactor secondFactor,
                                 PendingLogins pendingLogins) {
        this.loadUserPort = loadUserPort;
        this.passwordVerifier = passwordVerifier;
        this.accessTokenIssuer = accessTokenIssuer;
        this.refreshTokenStore = refreshTokenStore;
        this.loginAttempts = loginAttempts;
        this.secondFactor = secondFactor;
        this.pendingLogins = pendingLogins;
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
        if (secondFactor.requiredFor(user.get())) {
            // Not counted as a success yet: the failure count must keep running through the second step, or
            // someone with the password could guess codes forever
            throw new SecondFactorRequiredException(pendingLogins.start(user.get().id()));
        }
        loginAttempts.succeeded(command.email());
        return startSession(user.get(), command.client());
    }

    @Override
    public AuthTokens completeLogin(SecondFactorCommand command) {
        User user = pendingLogins.find(command.loginToken())
                .flatMap(loadUserPort::findById)
                .filter(User::isEnabled)
                .orElseThrow(LoginExpiredException::new);
        Optional<Duration> blocked = loginAttempts.blockedFor(user.email());
        if (blocked.isPresent()) {
            throw new TooManyLoginAttemptsException(blocked.get());
        }
        if (!secondFactor.check(user, command.code())) {
            pendingLogins.failed(command.loginToken());
            loginAttempts.failed(user.email());   // the same limit as wrong passwords, per account
            throw new WrongTwoFactorCodeException();
        }
        if (!pendingLogins.finish(command.loginToken())) {
            throw new LoginExpiredException();   // a second request with the same token, a moment too late
        }
        loginAttempts.succeeded(user.email());
        return startSession(user, command.client());
    }

    @Override
    public AuthTokens refresh(String refreshToken, ClientInfo client) {
        SessionRef session = refreshTokenStore.consume(refreshToken).orElseThrow(InvalidRefreshTokenException::new);
        Optional<User> user = loadUserPort.findById(session.userId()).filter(User::isEnabled);
        if (user.isEmpty()) {
            refreshTokenStore.end(session.userId(), session.sessionId());
            throw new InvalidRefreshTokenException();
        }
        return tokens(user.get(), session.sessionId(), refreshTokenStore.renew(session, client));
    }

    @Override
    public void logout(String refreshToken) {
        refreshTokenStore.revoke(refreshToken);
    }

    private AuthTokens startSession(User user, ClientInfo client) {
        RefreshGrant grant = refreshTokenStore.start(user.id(), client);
        return tokens(user, grant.sessionId(), grant.token());
    }

    private AuthTokens tokens(User user, UUID sessionId, String refreshToken) {
        AccessToken accessToken = accessTokenIssuer.issue(user, sessionId);
        return new AuthTokens(accessToken.value(), accessToken.expiresInSeconds(), refreshToken);
    }
}
