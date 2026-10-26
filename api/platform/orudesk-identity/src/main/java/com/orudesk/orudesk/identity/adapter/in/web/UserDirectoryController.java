package com.orudesk.orudesk.identity.adapter.in.web;

import com.orudesk.orudesk.identity.application.port.in.ListUsersQuery;
import com.orudesk.orudesk.identity.application.port.in.UserPage;
import com.orudesk.orudesk.identity.application.port.in.UserSearch;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The read side of user administration: the user list. */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
class UserDirectoryController {

    private final ListUsersQuery listUsers;

    UserDirectoryController(ListUsersQuery listUsers) {
        this.listUsers = listUsers;
    }

    /** GET /api/v1/users?q=asha&size=20&after=... : one page of the tenant's users, sorted by email. */
    @GetMapping
    @Operation(operationId = "listUsers", summary = "The tenant's users with their roles, one page at a time")
    @PreAuthorize("hasRole('ADMIN')")
    UserPage list(@RequestParam(required = false) @Nullable String q,
                  @RequestParam(required = false) @Nullable String after,
                  @RequestParam(defaultValue = "20") int size) {
        return listUsers.listUsers(new UserSearch(q, after, size));
    }

}
