package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.GetUserProfileQuery;
import com.orudesk.orudesk.identity.application.port.in.UserProfile;
import com.orudesk.orudesk.identity.application.port.out.LoadUserPort;
import com.orudesk.orudesk.identity.domain.OrgUnitId;
import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.User;
import com.orudesk.orudesk.identity.domain.UserId;
import com.orudesk.orudesk.identity.domain.UserNotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

@Service
@Transactional(readOnly = true)
public class UserProfileService implements GetUserProfileQuery {

    // "-v2": UserProfile gained a field on Day 19. A new cache name means old entries, written in the old
    // shape, are never read back; they expire on their own.
    public static final String CACHE = "user-profiles-v2";

    private final LoadUserPort loadUserPort;

    public UserProfileService(LoadUserPort loadUserPort) {
        this.loadUserPort = loadUserPort;
    }

    /** The first call per user reads the database; later calls are answered from Redis until evicted or expired. */
    @Override
    @Cacheable(cacheNames = CACHE, key = "#userId.value()")
    public UserProfile getProfile(UserId userId) {
        User user = loadUserPort.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        return new UserProfile(
                user.id().value(),
                user.tenantId().value(),
                user.email().value(),
                user.roles().stream().map(RoleCode::value).sorted().toList(),
                user.grants().stream()
                        .map(grant -> new UserProfile.Grant(grant.role().value(), grant.scopeId().map(OrgUnitId::value).orElse(null)))
                        .sorted(Comparator.comparing(UserProfile.Grant::role))
                        .toList(),
                user.isEnabled());
    }
}
