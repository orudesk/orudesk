package com.orudesk.orudesk.identity.adapter.in.web;

import com.orudesk.orudesk.identity.CurrentOrgUnit;
import com.orudesk.orudesk.identity.OrgUnitAccess;
import com.orudesk.orudesk.identity.application.port.in.OrgAccessQuery;
import com.orudesk.orudesk.identity.application.port.out.LoadOrgTreePort;
import com.orudesk.orudesk.identity.domain.MissingOrgUnitException;
import com.orudesk.orudesk.identity.domain.OrgUnitId;
import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.UserId;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The web side of scoped roles: who is calling (from the JWT) and for which unit (from the X-Org-Unit header).
 * Named "orgAccess" so @PreAuthorize can call it: @PreAuthorize("@orgAccess.currentUserHasRoleAt('ADMIN', #unitId)").
 */
@Component("orgAccess")
class OrgUnitAccessAdapter implements OrgUnitAccess, CurrentOrgUnit {

    private final OrgAccessQuery orgAccess;
    private final LoadOrgTreePort loadOrgTreePort;
    private final HttpServletRequest request;   // a proxy: each call reaches the request being handled right now

    OrgUnitAccessAdapter(OrgAccessQuery orgAccess, LoadOrgTreePort loadOrgTreePort, HttpServletRequest request) {
        this.orgAccess = orgAccess;
        this.loadOrgTreePort = loadOrgTreePort;
        this.request = request;
    }

    @Override
    public boolean currentUserHasRoleAt(String role, UUID orgUnitId) {
        return orgAccess.accessAt(currentUser(), new OrgUnitId(orgUnitId)).roles().contains(new RoleCode(role).value());
    }

    @Override
    public Set<UUID> unitsWhereCurrentUserHas(String role) {
        return orgAccess.unitsWithRole(currentUser(), new RoleCode(role)).stream()
                .map(OrgUnitId::value)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public UUID require() {
        String header = request.getHeader(HEADER);
        if (header == null || header.isBlank()) {
            throw new MissingOrgUnitException(HEADER);
        }
        OrgUnitId unit = new OrgUnitId(UUID.fromString(header.trim()));   // not a UUID: IllegalArgumentException, a 400
        loadOrgTreePort.loadTree().get(unit);                             // not in this tenant: a 404
        return unit.value();
    }

    /** A convenience for @PreAuthorize: the role at the unit in the X-Org-Unit header. */
    public boolean hasRoleHere(String role) {
        return currentUserHasRoleAt(role, require());
    }

    static UserId currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return new UserId(UUID.fromString(jwt.getToken().getSubject()));
        }
        throw new IllegalStateException("scoped roles need a logged-in user with an access token");
    }
}
