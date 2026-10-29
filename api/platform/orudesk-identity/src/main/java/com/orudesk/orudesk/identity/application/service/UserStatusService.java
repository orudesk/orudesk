package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.UserDisabled;
import com.orudesk.orudesk.identity.application.port.in.UserEnabled;
import com.orudesk.orudesk.identity.application.port.in.UserStatusUseCase;
import com.orudesk.orudesk.identity.application.port.out.BlockedUsers;
import com.orudesk.orudesk.identity.application.port.out.LoadUserPort;
import com.orudesk.orudesk.identity.application.port.out.SaveUserPort;
import com.orudesk.orudesk.identity.domain.SelfLockoutException;
import com.orudesk.orudesk.identity.domain.User;
import com.orudesk.orudesk.identity.domain.UserId;
import com.orudesk.orudesk.identity.domain.UserNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Disabling is three things: the flag in the database (no new login, no token refresh), the blocked list (the
 * access token in their browser stops working now, not in 15 minutes), and an audit entry.
 */
@Service
@Transactional
public class UserStatusService implements UserStatusUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final BlockedUsers blockedUsers;
    private final ApplicationEventPublisher events;

    public UserStatusService(LoadUserPort loadUserPort, SaveUserPort saveUserPort, BlockedUsers blockedUsers,
                             ApplicationEventPublisher events) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.blockedUsers = blockedUsers;
        this.events = events;
    }

    @Override
    public void disable(UserId userId, UserId disabledBy) {
        if (userId.equals(disabledBy)) {
            throw new SelfLockoutException("you cannot disable your own login; ask another admin");
        }
        User user = loadUserPort.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        // Blocked even if already disabled: cheap, and it repairs a block that expired or was lost with Redis
        blockedUsers.block(userId);
        if (!user.isEnabled()) {
            return;
        }
        user.disable();
        saveUserPort.save(user);
        events.publishEvent(new UserDisabled(user.tenantId().value(), userId.value(), user.email().value()));
    }

    @Override
    public void enable(UserId userId) {
        User user = loadUserPort.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (user.isEnabled()) {
            return;
        }
        user.enable();
        saveUserPort.save(user);
        // Their old tokens are long gone or soon will be; new ones from a fresh login must work at once
        blockedUsers.unblock(userId);
        events.publishEvent(new UserEnabled(user.tenantId().value(), userId.value(), user.email().value()));
    }
}
