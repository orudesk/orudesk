package com.orudesk.orudesk.party.adapter.in.web;

import com.orudesk.orudesk.party.domain.PersonNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns party's business exceptions into problem detail responses. */
@RestControllerAdvice
class PartyExceptionHandler {

    @ExceptionHandler(PersonNotFoundException.class)
    ProblemDetail personNotFound(PersonNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Person not found");
        return problem;
    }
}
