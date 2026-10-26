package com.orudesk.orudesk.identity.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * The JSON body of POST /api/v1/users/{id}/roles.
 *
 * @param orgUnitId where the role applies; leave it out for the whole tenant
 */
record AssignRoleRequest(@NotBlank String role, UUID orgUnitId) {
}
