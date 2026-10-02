package com.ourosapp.springapi.dto.internal;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de resposta para o endpoint de redefinição interna de senha.
 *
 * @param message Mensagem de confirmação da operação
 */
@Schema(description = "Resposta de sucesso para redefinição interna de senha")
public record PasswordResetInternalResponseDTO(
        @Schema(description = "Mensagem informativa", example = "Senha atualizada com sucesso.")
        String message
) {}
