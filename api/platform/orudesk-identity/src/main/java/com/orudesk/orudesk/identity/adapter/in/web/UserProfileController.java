package com.orudesk.orudesk.identity.adapter.in.web;

import com.orudesk.orudesk.identity.application.port.in.GetUserProfileQuery;
import com.orudesk.orudesk.identity.application.port.in.UserProfile;
import com.orudesk.orudesk.identity.domain.UserId;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
class UserProfileController {

    private final GetUserProfileQuery getUserProfile;

    UserProfileController(GetUserProfileQuery getUserProfile) {
        this.getUserProfile = getUserProfile;
    }

    /** Who am I? Any logged-in user may ask, about themselves only. */
    @GetMapping("/me")
    UserProfile me(@AuthenticationPrincipal Jwt jwt) {
        return getUserProfile.getProfile(new UserId(UUID.fromString(jwt.getSubject())));
    }
}
