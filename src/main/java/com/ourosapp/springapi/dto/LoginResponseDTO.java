package com.ourosapp.springapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de resposta para operações de login bem-sucedidas.
 *
 * @param token token JWT gerado para autenticação
 */
@Schema(description = "Resposta contendo o token de autenticação JWT")
public record LoginResponseDTO(
    @Schema(description = "Token JWT gerado", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String token
) {}
