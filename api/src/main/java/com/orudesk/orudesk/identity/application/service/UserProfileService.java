package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.GetUserProfileQuery;
import com.orudesk.orudesk.identity.application.port.in.UserProfile;
import com.orudesk.orudesk.identity.application.port.out.LoadUserPort;
import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.User;
import com.orudesk.orudesk.identity.domain.UserId;
import com.orudesk.orudesk.identity.domain.UserNotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserProfileService implements GetUserProfileQuery {

    public static final String CACHE = "user-profiles";

    private final LoadUserPort loadUserPort;

    public UserProfileService(LoadUserPort loadUserPort) {
        this.loadUserPort = loadUserPort;
    }

    /** The first call per user reads MariaDB; later calls are answered from Redis until evicted or expired. */
    @Override
    @Cacheable(cacheNames = CACHE, key = "#userId.value()")
    public UserProfile getProfile(UserId userId) {
        User user = loadUserPort.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        return new UserProfile(
                user.id().value(),
                user.tenantId().value(),
                user.email().value(),
                user.roles().stream().map(RoleCode::value).sorted().toList(),
                user.isEnabled());
    }
}
