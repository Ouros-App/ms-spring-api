package com.ourosapp.springapi.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para {@link GlobalExceptionHandler}.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Deve tratar DataIntegrityViolationException e retornar ProblemDetail com status 409 Conflict")
    void testHandleDataIntegrityViolationException() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException("Unique constraint violation");

        ProblemDetail result = exceptionHandler.handleDataIntegrityViolationException(exception);

        assertNotNull(result);
        assertEquals(HttpStatus.CONFLICT.value(), result.getStatus());
        assertEquals("Data Integrity Violation", result.getTitle());
        assertTrue(result.getDetail().contains("Conflito de integridade de dados"));
        assertNotNull(result.getProperties());
        assertTrue(result.getProperties().containsKey("timestamp"));
    }
}
