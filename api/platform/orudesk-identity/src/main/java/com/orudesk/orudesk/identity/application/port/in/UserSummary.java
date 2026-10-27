package com.orudesk.orudesk.identity.application.port.in;

import java.util.List;
import java.util.UUID;

/** One user in the user list: who, whether they may log in, and their roles (tenant-wide or at a unit). */
public record UserSummary(UUID id, String email, boolean enabled, List<UserProfile.Grant> grants) {
}
