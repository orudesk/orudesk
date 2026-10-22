package com.orudesk.orudesk.identity.application.port.in;

import com.orudesk.orudesk.identity.domain.UserId;

public interface GetUserProfileQuery {

    UserProfile getProfile(UserId userId);
}
