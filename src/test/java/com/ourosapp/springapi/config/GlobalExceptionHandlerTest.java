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

    @Test
    @DisplayName("Deve tratar ResponseStatusException e retornar ProblemDetail com status e mensagem da regra")
    void testHandleResponseStatusException() {
        org.springframework.web.server.ResponseStatusException exception =
                new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Funcionário não tem permissão para cadastrar produtor rural em fazenda de outra empresa integradora"
                );

        ProblemDetail result = exceptionHandler.handleResponseStatusException(exception);

        assertNotNull(result);
        assertEquals(HttpStatus.FORBIDDEN.value(), result.getStatus());
        assertEquals("Funcionário não tem permissão para cadastrar produtor rural em fazenda de outra empresa integradora", result.getDetail());
        assertNotNull(result.getProperties());
        assertTrue(result.getProperties().containsKey("timestamp"));
    }

    @Test
    @DisplayName("Deve tratar MethodArgumentNotValidException e retornar ProblemDetail com mapa de erros de campo")
    void testHandleMethodArgumentNotValidException() {
        org.springframework.validation.BeanPropertyBindingResult bindingResult =
                new org.springframework.validation.BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new org.springframework.validation.FieldError("target", "email", "O e-mail não pode estar em branco"));

        org.springframework.web.bind.MethodArgumentNotValidException exception =
                new org.springframework.web.bind.MethodArgumentNotValidException(null, bindingResult);

        ProblemDetail result = exceptionHandler.handleMethodArgumentNotValidException(exception);

        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatus());
        assertEquals("Erro de validação nos campos da requisição", result.getDetail());
        assertNotNull(result.getProperties());
        assertTrue(result.getProperties().containsKey("errors"));
    }
}
