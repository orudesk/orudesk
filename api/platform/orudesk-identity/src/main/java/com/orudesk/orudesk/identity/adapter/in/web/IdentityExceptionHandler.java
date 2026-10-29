package com.orudesk.orudesk.identity.adapter.in.web;

import com.orudesk.orudesk.identity.domain.EmailAlreadyRegisteredException;
import com.orudesk.orudesk.identity.domain.InvalidCredentialsException;
import com.orudesk.orudesk.identity.domain.InvalidPasswordResetTokenException;
import com.orudesk.orudesk.identity.domain.InvalidRefreshTokenException;
import com.orudesk.orudesk.identity.domain.LoginExpiredException;
import com.orudesk.orudesk.identity.domain.MissingOrgUnitException;
import com.orudesk.orudesk.identity.domain.OrgUnitCodeTakenException;
import com.orudesk.orudesk.identity.domain.OrgUnitNotFoundException;
import com.orudesk.orudesk.identity.domain.RoleChangeNotAllowedException;
import com.orudesk.orudesk.identity.domain.SecondFactorRequiredException;
import com.orudesk.orudesk.identity.domain.SelfLockoutException;
import com.orudesk.orudesk.identity.domain.SessionNotFoundException;
import com.orudesk.orudesk.identity.domain.TooManyLoginAttemptsException;
import com.orudesk.orudesk.identity.domain.TwoFactorAlreadyEnabledException;
import com.orudesk.orudesk.identity.domain.TwoFactorNotAvailableException;
import com.orudesk.orudesk.identity.domain.UnknownRoleException;
import com.orudesk.orudesk.identity.domain.UserNotFoundException;
import com.orudesk.orudesk.identity.domain.WrongPasswordException;
import com.orudesk.orudesk.identity.domain.WrongTwoFactorCodeException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** Turns identity's business exceptions into RFC 9457 "problem detail" JSON responses. */
@RestControllerAdvice
class IdentityExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ProblemDetail emailTaken(EmailAlreadyRegisteredException e) {
        return problem(HttpStatus.CONFLICT, "Email already registered", e.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    ProblemDetail userNotFound(UserNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "User not found", e.getMessage());
    }

    @ExceptionHandler(SessionNotFoundException.class)
    ProblemDetail sessionNotFound(SessionNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Session not found", e.getMessage());
    }

    /**
     * A wrong current password or code is a field error (400), like any form mistake: the user is logged in, and a
     * 401 would make the web app think their session ended (Day 67).
     */
    @ExceptionHandler(WrongPasswordException.class)
    ProblemDetail wrongPassword(WrongPasswordException e) {
        return fieldError("password", "That's not your current password");
    }

    @ExceptionHandler(WrongTwoFactorCodeException.class)
    ProblemDetail wrongCode(WrongTwoFactorCodeException e) {
        return fieldError("code", "That code isn't right, or was already used. Try the newest one");
    }

    @ExceptionHandler(TwoFactorAlreadyEnabledException.class)
    ProblemDetail twoFactorOn(TwoFactorAlreadyEnabledException e) {
        return problem(HttpStatus.CONFLICT, "Two-factor sign-in already on", e.getMessage());
    }

    @ExceptionHandler(TwoFactorNotAvailableException.class)
    ProblemDetail twoFactorUnavailable(TwoFactorNotAvailableException e) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Two-factor sign-in not available", e.getMessage());
    }

    @ExceptionHandler(UnknownRoleException.class)
    ProblemDetail unknownRole(UnknownRoleException e) {
        return problem(HttpStatus.BAD_REQUEST, "Unknown role", e.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail invalidCredentials(InvalidCredentialsException e) {
        return problem(HttpStatus.UNAUTHORIZED, "Login failed", e.getMessage());
    }

    /** 429 with Retry-After (in seconds), the standard way to say "slow down" that clients and proxies understand. */
    /** 401 with the token for the second step: the client shows the code field instead of an error (Day 68). */
    @ExceptionHandler(SecondFactorRequiredException.class)
    ProblemDetail secondFactorRequired(SecondFactorRequiredException e) {
        ProblemDetail problem = problem(HttpStatus.UNAUTHORIZED, "Second factor required", e.getMessage());
        problem.setProperty("loginToken", e.loginToken());
        return problem;
    }

    @ExceptionHandler(LoginExpiredException.class)
    ProblemDetail loginExpired(LoginExpiredException e) {
        return problem(HttpStatus.UNAUTHORIZED, "Login expired", e.getMessage());
    }

    @ExceptionHandler(TooManyLoginAttemptsException.class)
    ResponseEntity<ProblemDetail> tooManyLoginAttempts(TooManyLoginAttemptsException e) {
        long seconds = Math.max(1, e.retryAfter().toSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(seconds))
                .body(problem(HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts", e.getMessage()));
    }

    @ExceptionHandler(InvalidPasswordResetTokenException.class)
    ProblemDetail invalidResetToken(InvalidPasswordResetTokenException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid reset link", e.getMessage());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    ProblemDetail invalidRefreshToken(InvalidRefreshTokenException e) {
        return problem(HttpStatus.UNAUTHORIZED, "Invalid refresh token", e.getMessage());
    }

    @ExceptionHandler(OrgUnitNotFoundException.class)
    ProblemDetail orgUnitNotFound(OrgUnitNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Org unit not found", e.getMessage());
    }

    @ExceptionHandler(OrgUnitCodeTakenException.class)
    ProblemDetail orgUnitCodeTaken(OrgUnitCodeTakenException e) {
        return problem(HttpStatus.CONFLICT, "Org unit code taken", e.getMessage());
    }

    @ExceptionHandler(MissingOrgUnitException.class)
    ProblemDetail missingOrgUnit(MissingOrgUnitException e) {
        return problem(HttpStatus.BAD_REQUEST, "Org unit missing", e.getMessage());
    }

    @ExceptionHandler(RoleChangeNotAllowedException.class)
    ProblemDetail roleChangeNotAllowed(RoleChangeNotAllowedException e) {
        return problem(HttpStatus.CONFLICT, "Role change not allowed", e.getMessage());
    }

    @ExceptionHandler(SelfLockoutException.class)
    ProblemDetail selfLockout(SelfLockoutException e) {
        return problem(HttpStatus.CONFLICT, "Not allowed on your own account", e.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }

    /** The same shape as a @Valid failure, so clients show it next to the field. */
    private static ProblemDetail fieldError(String field, String message) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "some fields are invalid");
        problem.setTitle("Validation failed");
        problem.setProperty("errors", Map.of(field, message));
        return problem;
    }
}
