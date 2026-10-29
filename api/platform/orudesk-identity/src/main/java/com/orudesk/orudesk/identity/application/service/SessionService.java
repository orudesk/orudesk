package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.ActiveSession;
import com.orudesk.orudesk.identity.application.port.in.SessionsEnded;
import com.orudesk.orudesk.identity.application.port.in.SessionsUseCase;
import com.orudesk.orudesk.identity.application.port.out.LoadUserPort;
import com.orudesk.orudesk.identity.application.port.out.RefreshTokenStore;
import com.orudesk.orudesk.identity.domain.SessionNotFoundException;
import com.orudesk.orudesk.identity.domain.User;
import com.orudesk.orudesk.identity.domain.UserId;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Day 66. Sessions live in the refresh token store (Redis); this service adds the rules: a user sees and ends only
 * their own, and every ending is audited. Ending a session stops its access tokens at once too (the store is
 * asked for every request that names a session).
 */
@Service
public class SessionService implements SessionsUseCase {

    private final RefreshTokenStore store;
    private final LoadUserPort loadUserPort;
    private final ApplicationEventPublisher events;

    public SessionService(RefreshTokenStore store, LoadUserPort loadUserPort, ApplicationEventPublisher events) {
        this.store = store;
        this.loadUserPort = loadUserPort;
        this.events = events;
    }

    @Override
    public List<ActiveSession> list(UserId userId, @Nullable UUID current) {
        return store.sessions(userId).stream()
                .map(s -> new ActiveSession(s.id(), s.startedAt(), s.lastUsedAt(), s.device(), s.ipAddress(),
                        s.id().equals(current)))
                .toList();
    }

    @Override
    @Transactional   // the audit entry is written in this transaction (the audit module's listener)
    public void end(UserId userId, UUID sessionId) {
        if (!store.end(userId, sessionId)) {
            throw new SessionNotFoundException(sessionId);
        }
        audit(userId, 1);
    }

    @Override
    @Transactional
    public int endOthers(UserId userId, @Nullable UUID current) {
        int ended = store.endAllExcept(userId, current);
        if (ended > 0) {
            audit(userId, ended);
        }
        return ended;
    }

    private void audit(UserId userId, int count) {
        loadUserPort.findById(userId).ifPresent((User user) -> events.publishEvent(new SessionsEnded(
                user.tenantId().value(), userId.value(), user.email().value(), count)));
    }
}
