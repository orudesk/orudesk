package com.orudesk.orudesk.identity.application.port.in;

import java.util.List;

/** Every role there is: the core's and the ones modules declared (RoleDefinition), sorted by code. */
public interface ListRolesQuery {

    List<RoleDetails> listRoles();
}
