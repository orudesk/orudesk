package com.orudesk.orudesk.identity.application.port.out;

import com.orudesk.orudesk.identity.domain.User;

public interface AccessTokenIssuer {

    AccessToken issue(User user);
}
