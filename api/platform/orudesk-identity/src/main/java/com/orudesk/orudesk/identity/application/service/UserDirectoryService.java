package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.ListRolesQuery;
import com.orudesk.orudesk.identity.application.port.in.ListUsersQuery;
import com.orudesk.orudesk.identity.application.port.in.RoleDetails;
import com.orudesk.orudesk.identity.application.port.in.UserPage;
import com.orudesk.orudesk.identity.application.port.in.UserProfile;
import com.orudesk.orudesk.identity.application.port.in.UserSearch;
import com.orudesk.orudesk.identity.application.port.in.UserSummary;
import com.orudesk.orudesk.identity.application.port.out.LoadRolesPort;
import com.orudesk.orudesk.identity.application.port.out.SearchUsersPort;
import com.orudesk.orudesk.identity.domain.OrgUnitId;
import com.orudesk.orudesk.identity.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/** Read-only: who the tenant's users are, and which roles exist. The admin screens are built on these. */
@Service
@Transactional(readOnly = true)
public class UserDirectoryService implements ListUsersQuery, ListRolesQuery {

    private final SearchUsersPort searchUsersPort;
    private final LoadRolesPort loadRolesPort;

    public UserDirectoryService(SearchUsersPort searchUsersPort, LoadRolesPort loadRolesPort) {
        this.searchUsersPort = searchUsersPort;
        this.loadRolesPort = loadRolesPort;
    }

    @Override
    public UserPage listUsers(UserSearch search) {
        String after = search.after() == null ? null : UserCursor.decode(search.after());
        // Ask for one more than a page: if it comes back, there is a next page (no COUNT query needed)
        List<User> found = searchUsersPort.search(search.text(), after, search.size() + 1);
        boolean more = found.size() > search.size();
        List<User> page = more ? found.subList(0, search.size()) : found;
        String next = more ? UserCursor.encode(page.getLast().email().value()) : null;
        return new UserPage(page.stream().map(UserDirectoryService::summary).toList(), next);
    }

    @Override
    public List<RoleDetails> listRoles() {
        return loadRolesPort.loadAll();
    }

    private static UserSummary summary(User user) {
        return new UserSummary(
                user.id().value(),
                user.email().value(),
                user.isEnabled(),
                user.grants().stream()
                        .map(grant -> new UserProfile.Grant(grant.role().value(), grant.scopeId().map(OrgUnitId::value).orElse(null)))
                        .sorted(Comparator.comparing(UserProfile.Grant::role)
                                .thenComparing(grant -> grant.orgUnitId() == null ? "" : grant.orgUnitId().toString()))
                        .toList());
    }
}
