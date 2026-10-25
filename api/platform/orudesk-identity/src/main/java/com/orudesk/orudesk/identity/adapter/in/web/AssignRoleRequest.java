package com.orudesk.orudesk.identity.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

/** The JSON body of POST /api/v1/users/{id}/roles. */
record AssignRoleRequest(@NotBlank String role) {
}
