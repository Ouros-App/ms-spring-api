package com.ourosapp.springapi.dto.companyemployee;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de requisição para atualização parcial de Funcionário da Empresa Integradora (PATCH).
 * Todos os campos são opcionais, permitindo atualizar apenas o que for fornecido.
 *
 * @param email     Novo e-mail corporativo (opcional)
 * @param telephone Novo telefone de contato (opcional, entre 10 e 13 dígitos)
 * @param password  Nova senha de acesso (opcional, entre 8 e 20 caracteres com requisitos de complexidade)
 */
@Schema(description = "Dados para atualização parcial do funcionário da empresa integradora")
public record CompanyEmployeeUpdateDTO(

        @Schema(description = "Novo e-mail corporativo do funcionário", example = "joao.novo@empresa.com.br")
        @Email(message = "Formato de e-mail inválido")
        @Size(max = 50, message = "O e-mail deve ter no máximo 50 caracteres")
        String email,

        @Schema(description = "Novo telefone de contato (entre 10 e 13 dígitos numéricos)", example = "11988887777")
        @Pattern(regexp = "^$|^\\d{10,13}$", message = "O telefone deve conter apenas números e ter entre 10 e 13 dígitos")
        String telephone,

        @Schema(description = "Nova senha de acesso", example = "NovaSenha@123")
        @Pattern(
                regexp = "^$|^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,20}$",
                message = "A senha deve ter entre 8 e 20 caracteres, incluindo pelo menos uma letra maiúscula, uma minúscula, um número e um caractere especial"
        )
        String password
) {

    /**
     * Construtor compacto para sanitização de espaços em branco e normalização do e-mail.
     */
    public CompanyEmployeeUpdateDTO {
        email = email != null ? email.trim().toLowerCase() : null;
        telephone = telephone != null ? telephone.trim() : null;
    }

    /**
     * Verifica se pelo menos um dos campos foi informado para atualização.
     *
     * @return {@code true} se houver pelo menos um campo não nulo e não vazio
     */
    public boolean hasUpdates() {
        return (email != null && !email.isBlank())
                || (telephone != null && !telephone.isBlank())
                || (password != null && !password.isBlank());
    }
}
