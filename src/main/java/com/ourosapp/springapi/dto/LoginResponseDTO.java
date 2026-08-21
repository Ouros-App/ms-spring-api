package com.ourosapp.springapi.dto;

/**
 * DTO de resposta para operações de login bem-sucedidas.
 *
 * @param token token JWT gerado para autenticação
 */
public record LoginResponseDTO(
    String token
) {}
