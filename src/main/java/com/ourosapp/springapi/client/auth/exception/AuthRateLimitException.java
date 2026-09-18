package com.ourosapp.springapi.client.auth.exception;

import lombok.Getter;

/**
 * Exceção lançada quando o ms-auth-service responde com HTTP 429 Too Many Requests.
 */
@Getter
public class AuthRateLimitException extends RuntimeException {

    private final Long retryAfterSeconds;

    public AuthRateLimitException(String message, Long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
