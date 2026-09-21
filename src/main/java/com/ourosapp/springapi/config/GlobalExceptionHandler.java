package com.ourosapp.springapi.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Tratador global de exceções para padronização de respostas de erro da API REST.
 * Intercepta violações de integridade do banco de dados (chaves únicas, restrições de FK),
 * convertendo-as automaticamente para o status HTTP 409 (Conflict).
 */
@Slf4j
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
     * Intercepta {@link ResponseStatusException} lançadas pelas regras de negócio.
     * Retorna o status HTTP e a mensagem descritiva do motivo do erro em formato ProblemDetail (RFC 7807).
     *
     * @param ex exceção disparada
     * @return {@link ProblemDetail} formatado com a mensagem de negócio
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatusException(ResponseStatusException ex) {
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
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Erro de validação nos campos da requisição"
        );
        problemDetail.setTitle("Validation Failed");
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setProperty("timestamp", Instant.now());

        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        int globalErrorIndex = 1;
        for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            String key = ex.getBindingResult().getGlobalErrors().size() == 1
                    ? error.getObjectName()
                    : error.getObjectName() + "_" + (globalErrorIndex++);
            errors.put(key, error.getDefaultMessage());
        }
        problemDetail.setProperty("errors", errors);

        return problemDetail;
    }

    /**
     * Intercepta erros de leitura e desserialização de JSON na requisição (ex.: payload malformatado ou tipo incompatível).
     * Retorna status 400 Bad Request em formato ProblemDetail.
     *
     * @param ex exceção de leitura HTTP disparada pelo Spring MVC
     * @return {@link ProblemDetail} formatado com status 400
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadableException(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Corpo da requisição inválido ou malformatado"
        );
        problemDetail.setTitle("Malformed JSON Request");
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        return buildProblemDetail(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Método HTTP não suportado para esta rota"
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex) {
        return buildProblemDetail(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Tipo de mídia não suportado"
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        return buildProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Parâmetro obrigatório ausente: " + ex.getParameterName()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        return buildProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Valor inválido para o parâmetro: " + ex.getName()
        );
    }

    private ProblemDetail buildProblemDetail(HttpStatus status, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    /**
     * Intercepta quaisquer outras exceções não tratadas explicitamente,
     * garantindo log estruturado e resposta uniforme com status 500 em formato ProblemDetail.
     *
     * @param ex exceção inesperada disparada
     * @return {@link ProblemDetail} formatado com status 500
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) throws Exception {
        if (ex instanceof org.springframework.security.access.AccessDeniedException) {
            throw ex;
        }
        log.error("Erro interno inesperado na API: ", ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno inesperado no servidor."
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

}
