package com.ourosapp.springapi.dto.internal;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * DTO de requisição interna para redefinição de senha de usuários (POST /internal/v1/password-reset).
 *
 * @param accountType Tipo de conta do usuário (farm_owner ou company_employee)
 * @param id          Identificador numérico do usuário no banco de dados
 * @param newPassword Nova senha do usuário contendo requisitos de complexidade
 */
@Schema(description = "Dados para redefinição interna de senha de usuário via comunicação M2M")
public record PasswordResetInternalDTO(

        @Schema(description = "Tipo de conta do usuário (farm_owner ou company_employee)", example = "farm_owner")
        @JsonProperty("accountType")
        @JsonAlias({"account_type", "accounttype"})
        @NotBlank(message = "O tipo de conta é obrigatório")
        @Pattern(regexp = "^(farm_owner|company_employee)$", message = "O tipo de conta deve ser 'farm_owner' ou 'company_employee'")
        String accountType,

        @Schema(description = "Identificador do usuário no banco de dados", example = "10")
        @NotNull(message = "O ID é obrigatório")
        @Positive(message = "O ID deve ser maior que zero")
        Long id,

        @Schema(description = "Nova senha do usuário", example = "NovaSenhaForte@2026")
        @JsonProperty("newPassword")
        @JsonAlias({"new_password", "newpassword"})
        @NotBlank(message = "A nova senha não pode estar em branco")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,20}$",
                message = "A senha deve ter entre 8 e 20 caracteres, incluindo pelo menos uma letra maiúscula, uma minúscula, um número e um caractere especial"
        )
        String newPassword
) {
    /**
     * Sanitização automática dos campos.
     */
    public PasswordResetInternalDTO {
        accountType = accountType != null ? accountType.trim().toLowerCase() : null;
        newPassword = newPassword != null ? newPassword.trim() : null;
    }
}
