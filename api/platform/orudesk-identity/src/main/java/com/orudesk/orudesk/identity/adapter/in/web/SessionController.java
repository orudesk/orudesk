package com.orudesk.orudesk.identity.adapter.in.web;

import com.orudesk.orudesk.identity.application.port.in.ActiveSession;
import com.orudesk.orudesk.identity.application.port.in.SessionsUseCase;
import com.orudesk.orudesk.identity.domain.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Where am I signed in? Any logged-in user, about their own sessions only (Day 66). */
@RestController
@RequestMapping("/api/v1/users/me/sessions")
@Tag(name = "Users")
class SessionController {

    private final SessionsUseCase sessions;

    SessionController(SessionsUseCase sessions) {
        this.sessions = sessions;
    }

    @GetMapping
    @Operation(operationId = "listMySessions", summary = "The devices the logged-in user is signed in on, most recent first")
    List<ActiveSession> list(@AuthenticationPrincipal Jwt jwt) {
        return sessions.list(user(jwt), session(jwt));
    }

    @DeleteMapping("/{sessionId}")
    @Operation(operationId = "endMySession", summary = "Sign out one of the logged-in user's sessions")
    @ApiResponse(responseCode = "204", description = "Ended: its refresh and access tokens stop working")
    ResponseEntity<Void> end(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sessionId) {
        sessions.end(user(jwt), sessionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/end-others")
    @Operation(operationId = "endMyOtherSessions", summary = "Sign out everywhere except this session")
    @ApiResponse(responseCode = "204", description = "Every other session has ended")
    ResponseEntity<Void> endOthers(@AuthenticationPrincipal Jwt jwt) {
        sessions.endOthers(user(jwt), session(jwt));
        return ResponseEntity.noContent().build();
    }

    private static UserId user(Jwt jwt) {
        return new UserId(UUID.fromString(jwt.getSubject()));
    }

    /** Access tokens name their session from Day 66 on; an older one names none. */
    private static @Nullable UUID session(Jwt jwt) {
        String sid = jwt.getClaimAsString("sid");
        return sid == null ? null : UUID.fromString(sid);
    }
}
