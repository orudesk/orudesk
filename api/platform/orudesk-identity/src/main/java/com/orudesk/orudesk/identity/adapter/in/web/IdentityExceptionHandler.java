package com.orudesk.orudesk.identity.adapter.in.web;

import com.orudesk.orudesk.identity.domain.EmailAlreadyRegisteredException;
import com.orudesk.orudesk.identity.domain.InvalidCredentialsException;
import com.orudesk.orudesk.identity.domain.InvalidRefreshTokenException;
import com.orudesk.orudesk.identity.domain.MissingOrgUnitException;
import com.orudesk.orudesk.identity.domain.OrgUnitCodeTakenException;
import com.orudesk.orudesk.identity.domain.OrgUnitNotFoundException;
import com.orudesk.orudesk.identity.domain.RoleChangeNotAllowedException;
import com.orudesk.orudesk.identity.domain.UnknownRoleException;
import com.orudesk.orudesk.identity.domain.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(UnknownRoleException.class)
    ProblemDetail unknownRole(UnknownRoleException e) {
        return problem(HttpStatus.BAD_REQUEST, "Unknown role", e.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail invalidCredentials(InvalidCredentialsException e) {
        return problem(HttpStatus.UNAUTHORIZED, "Login failed", e.getMessage());
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

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
