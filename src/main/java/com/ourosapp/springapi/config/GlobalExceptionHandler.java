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

    /**
     * Intercepta {@link org.springframework.web.server.ResponseStatusException} lançadas pelas regras de negócio.
     * Retorna o status HTTP e a mensagem descritiva do motivo do erro em formato ProblemDetail (RFC 7807).
     *
     * @param ex exceção disparada
     * @return {@link ProblemDetail} formatado com a mensagem de negócio
     */
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ProblemDetail handleResponseStatusException(org.springframework.web.server.ResponseStatusException ex) {
        String reason = ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(ex.getStatusCode(), reason);
        problemDetail.setTitle(ex.getStatusCode().toString());
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    /**
     * Intercepta erros de validação de DTOs disparados pelo Jakarta Bean Validation (@Valid).
     * Retorna a lista dos erros de campos com mensagens claras.
     *
     * @param ex exceção de validação de argumentos
     * @return {@link ProblemDetail} com status 400 e os erros de cada campo
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValidException(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Erro de validação nos campos da requisição"
        );
        problemDetail.setTitle("Validation Failed");
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setProperty("timestamp", Instant.now());

        java.util.Map<String, String> errors = new java.util.HashMap<>();
        for (org.springframework.validation.FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        for (org.springframework.validation.ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            errors.put(error.getObjectName(), error.getDefaultMessage());
        }
        problemDetail.setProperty("errors", errors);

        return problemDetail;
    }
}
