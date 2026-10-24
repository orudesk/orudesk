package com.orudesk.orudesk.shared.adapter.in.web;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.UncategorizedDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Problem responses (RFC 9457) for errors any feature can raise.
 * Extending ResponseEntityExceptionHandler gives Spring MVC's own errors
 * (bad JSON, wrong HTTP method, ...) the same problem format.
 */
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /** @Valid failed: list every invalid field and why. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new TreeMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "some fields are invalid");
        problem.setTitle("Validation failed");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    /** A value object or command refused its input, for example new RoleCode("not a role!"). */
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidInput(IllegalArgumentException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setTitle("Invalid input");
        return problem;
    }

    /** The database refused the write, for example two registrations with the same email at once. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflict(DataIntegrityViolationException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "the data conflicts with existing data");
        problem.setTitle("Conflict");
        return problem;
    }

    /**
     * A service we depend on (Redis, the database) can't be reached. Not the client's fault and not a bug:
     * 503 says "try again soon", and Retry-After says when. A 500 would make the client give up.
     * A dead service shows up in three ways: no connection at all (DataAccessResourceFailureException),
     * a connection that stopped answering (QueryTimeoutException), or a connection that broke in the
     * middle of a command (see brokenConnection below).
     */
    @ExceptionHandler({DataAccessResourceFailureException.class, QueryTimeoutException.class})
    ResponseEntity<ProblemDetail> unavailable(RuntimeException e) {
        logger.warn("A backing service is unavailable: " + e.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "a service we depend on is unavailable, please try again shortly");
        problem.setTitle("Service unavailable");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "30")
                .body(problem);
    }

    /**
     * A connection that broke mid-command ("Connection reset") arrives as an uncategorized error, because
     * the client can't tell why the socket died. Only a network failure underneath makes it a 503; any other
     * uncategorized error is a real bug and stays a 500.
     */
    @ExceptionHandler(UncategorizedDataAccessException.class)
    ResponseEntity<ProblemDetail> brokenConnection(UncategorizedDataAccessException e) {
        if (e.getRootCause() instanceof IOException) {
            return unavailable(e);
        }
        throw e;
    }
}
