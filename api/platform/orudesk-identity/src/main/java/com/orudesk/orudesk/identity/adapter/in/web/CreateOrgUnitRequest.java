package com.orudesk.orudesk.identity.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** The JSON body of POST /api/v1/org-units. */
record CreateOrgUnitRequest(
        @NotNull UUID parentId,
        @NotBlank @Size(max = 50) String type,
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 200) String name) {
}
