package com.orudesk.orudesk.shared.adapter.in.web;

import com.orudesk.orudesk.shared.EditConflictException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.UncategorizedDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

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

    /** The client edited an old version of the record (Day 46): it reloads, and the user makes the change again. */
    @ExceptionHandler(EditConflictException.class)
    ProblemDetail editConflict(EditConflictException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setTitle("Edit conflict");
        problem.setProperty("currentVersion", e.currentVersion());
        return problem;
    }

    /**
     * The same, caught by the database instead: two saves of the same version raced each other, the first one won,
     * and the second one's "UPDATE ... WHERE version = 3" found no row. Rare (the two must overlap by milliseconds),
     * so the message is the same and nobody needs to know which check caught it.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail raceLost(OptimisticLockingFailureException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "this record was changed by someone else at the same moment; reload it and make your change again");
        problem.setTitle("Edit conflict");
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
     * A connection that broke mid-command arrives as an uncategorized error ("Connection reset", or Hibernate's
     * "Unable to rollback" when the database went away inside a transaction), and a pool that can't get a
     * connection fails before the transaction starts. Only a lost connection underneath makes these a 503; any
     * other such error is a real bug and stays a 500.
     */
    @ExceptionHandler({UncategorizedDataAccessException.class, TransactionException.class})
    ResponseEntity<ProblemDetail> brokenConnection(RuntimeException e) {
        if (ConnectionFailures.isConnectionFailure(e)) {
            return unavailable(e);
        }
        throw e;
    }
}
