package com.pivot.banking.transaction.infrastructure.adapter.in;

import com.pivot.banking.transaction.application.exception.MovementNotFoundException;
import com.pivot.banking.transaction.adapter.in.dto.Problem;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Unit tests for RFC 7807 error responses. */
class MovementExceptionHandlerTest {

    private final MovementExceptionHandler handler = new MovementExceptionHandler();

    @Test
    void mapsNotFoundAndIllegalArgumentErrors() {
        // Arrange
        var notFoundException = new MovementNotFoundException("movement-1");
        var invalidException = new IllegalArgumentException("Invalid cursor");

        // Act
        var notFound = handler.notFound(notFoundException);
        var invalid = handler.badRequest(invalidException);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatusCode());
        assertEquals("Movement not found", notFound.getBody().getTitle());
        assertEquals(HttpStatus.BAD_REQUEST, invalid.getStatusCode());
        assertEquals("Invalid cursor", invalid.getBody().getDetail());
    }

    @Test
    void mapsBodyAndParameterValidationErrors() {
        // Arrange
        WebExchangeBindException bodyException = mock(WebExchangeBindException.class);
        when(bodyException.getFieldErrors()).thenReturn(
                List.of(new FieldError("movementRequest", "amount", "must be positive")));
        var parameterException = new ConstraintViolationException("invalid page", Set.of());

        // Act
        ResponseEntity<Problem> body = handler.requestBodyValidation(bodyException);
        ResponseEntity<Problem> parameter = handler.requestParameterValidation(parameterException);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, body.getStatusCode());
        assertEquals(MediaType.valueOf("application/problem+json"), body.getHeaders().getContentType());
        assertEquals("amount: must be positive", body.getBody().getDetail());
        assertEquals(HttpStatus.BAD_REQUEST, parameter.getStatusCode());
        assertEquals("Request parameters are invalid", parameter.getBody().getDetail());
    }

    @Test
    void mapsUnreadableInputAndMongoFailures() {
        // Arrange
        ServerWebInputException inputException = new ServerWebInputException("Malformed JSON");
        DataAccessException databaseException = new DataAccessException("mongo unavailable") {
        };

        // Act
        ResponseEntity<Problem> input = handler.unreadableInput(inputException);
        ResponseEntity<Problem> persistence = handler.persistenceUnavailable(databaseException);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, input.getStatusCode());
        assertEquals("Malformed JSON", input.getBody().getDetail());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, persistence.getStatusCode());
        assertEquals("Movement storage unavailable", persistence.getBody().getTitle());
    }
}
