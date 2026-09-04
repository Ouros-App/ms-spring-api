package com.ourosapp.springapi.config;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Tratador global de exceções para padronização de respostas de erro da API REST.
 * Intercepta violações de integridade do banco de dados (chaves únicas, restrições de FK),
 * convertendo-as automaticamente para o status HTTP 409 (Conflict).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Intercepta {@link DataIntegrityViolationException} lançadas pelo Spring Data JPA / Hibernate
     * no momento da persistência ou commit da transação.
     *
     * @param ex exceção de integridade de dados disparada
     * @return {@link ProblemDetail} formatado com status 409 Conflict
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Conflito de integridade de dados ou registro duplicado no banco de dados"
        );
        problemDetail.setTitle("Data Integrity Violation");
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}
