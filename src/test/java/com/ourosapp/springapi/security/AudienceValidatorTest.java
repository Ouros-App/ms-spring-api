package com.ourosapp.springapi.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AudienceValidatorTest {

    @Test
    @DisplayName("Deve validar com sucesso quando o token contém a audience configurada")
    void testValidateAudienceSuccess() {
        AudienceValidator validator = new AudienceValidator("ms-spring-api");

        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of("aud", List.of("ms-spring-api", "ouros-mobile"))
        );

        OAuth2TokenValidatorResult result = validator.validate(jwt);
        assertFalse(result.hasErrors());
    }

    @Test
    @DisplayName("Deve falhar quando o token não contém a audience esperada")
    void testValidateAudienceFailure() {
        AudienceValidator validator = new AudienceValidator("ms-spring-api");

        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of("aud", List.of("other-api"))
        );

        OAuth2TokenValidatorResult result = validator.validate(jwt);
        assertTrue(result.hasErrors());
        assertEquals("O token JWT não contém a audience esperada para este recurso.",
                result.getErrors().iterator().next().getDescription());
    }

    @Test
    @DisplayName("Deve falhar quando o token não contém a claim aud")
    void testValidateAudienceMissing() {
        AudienceValidator validator = new AudienceValidator("ms-spring-api");

        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of("sub", "user-uuid")
        );

        OAuth2TokenValidatorResult result = validator.validate(jwt);
        assertTrue(result.hasErrors());
        assertEquals("O token JWT não contém a audience esperada para este recurso.",
                result.getErrors().iterator().next().getDescription());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando a audience for nula ou vazia na inicialização")
    void testInitializationWithoutRequiredAudience() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> new AudienceValidator(null));
        assertTrue(ex1.getMessage().contains("audience OAuth2 deve ser configurada"));

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> new AudienceValidator(""));
        assertTrue(ex2.getMessage().contains("audience OAuth2 deve ser configurada"));

        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () -> new AudienceValidator("   "));
        assertTrue(ex3.getMessage().contains("audience OAuth2 deve ser configurada"));
    }
}
