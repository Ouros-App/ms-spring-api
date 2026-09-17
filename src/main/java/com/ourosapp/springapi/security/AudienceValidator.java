package com.ourosapp.springapi.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Validador OAuth2 para garantir que o token JWT contenha a audience esperada deste microserviço.
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_AUDIENCE_ERROR =
            new OAuth2Error("invalid_token", "O token JWT não contém a audience esperada para este recurso.", null);

    private final String requiredAudience;

    public AudienceValidator(String requiredAudience) {
        this.requiredAudience = requiredAudience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (requiredAudience == null || requiredAudience.isBlank()) {
            return OAuth2TokenValidatorResult.success();
        }

        List<String> audience = jwt.getAudience();
        if (audience != null && audience.contains(requiredAudience)) {
            return OAuth2TokenValidatorResult.success();
        }

        return OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE_ERROR);
    }
}
