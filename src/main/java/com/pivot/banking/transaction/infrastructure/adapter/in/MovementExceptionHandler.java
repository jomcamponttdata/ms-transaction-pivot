package com.pivot.banking.transaction.infrastructure.adapter.in;

import com.pivot.banking.transaction.adapter.in.dto.Problem;
import com.pivot.banking.transaction.application.exception.MovementNotFoundException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps movement application and persistence errors to RFC 7807 HTTP responses. */
@RestControllerAdvice
public class MovementExceptionHandler {

    /** Returns a not-found problem when a movement does not exist. */
    @ExceptionHandler(MovementNotFoundException.class)
    public ResponseEntity<Problem> notFound(MovementNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Movement not found", exception.getMessage());
    }

    /** Returns a bad-request problem for invalid filters, cursors, or movement fields. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Problem> badRequest(IllegalArgumentException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage());
    }

    /** Returns service-unavailable when MongoDB cannot complete a persistence operation. */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Problem> persistenceUnavailable(DataAccessException exception) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Movement storage unavailable",
                "The movement could not be persisted at this time");
    }

    private ResponseEntity<Problem> problem(HttpStatus status, String title, String detail) {
        var body = new Problem(title, status.value()).type("about:blank").detail(detail);
        return ResponseEntity.status(status)
                .contentType(MediaType.valueOf("application/problem+json"))
                .body(body);
    }
}
