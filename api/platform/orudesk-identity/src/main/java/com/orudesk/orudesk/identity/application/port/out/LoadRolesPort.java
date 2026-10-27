package com.orudesk.orudesk.identity.application.port.out;

import com.orudesk.orudesk.identity.application.port.in.RoleDetails;

import java.util.List;

public interface LoadRolesPort {

    /** Every role, sorted by code. */
    List<RoleDetails> loadAll();
}
