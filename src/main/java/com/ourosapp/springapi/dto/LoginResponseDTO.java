package com.ourosapp.springapi.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de resposta para operações de login bem-sucedidas.
 *
 * @param token       token JWT gerado para autenticação
 * @param firstAccess indicador de primeiro acesso do usuário (aplicável a produtores rurais)
 */
@Schema(description = "Resposta contendo o token de autenticação JWT e informações de acesso")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponseDTO(
        @Schema(description = "Token JWT gerado", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String token,

        @Schema(description = "Indica se é o primeiro acesso do usuário ao sistema (aplicável a produtores rurais)", example = "true")
        @JsonProperty("first_access")
        @JsonAlias({"firstAccess", "first_acess", "firstAcess"})
        Boolean firstAccess
) {
    /**
     * Construtor de conveniência para respostas de login sem informação de primeiro acesso.
     *
     * @param token token JWT gerado
     */
    public LoginResponseDTO(String token) {
        this(token, null);
    }
}
