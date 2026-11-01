package com.ourosapp.springapi.dto.companyemployee;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de requisição para atualização parcial de Funcionário da Empresa Integradora (PATCH).
 * Todos os campos são opcionais, permitindo atualizar apenas o que for fornecido.
 *
 * @param name      Novo nome completo do funcionário (opcional)
 * @param email     Novo e-mail corporativo (opcional)
 * @param telephone Novo telefone de contato (opcional, entre 10 e 13 dígitos)
 * @param password  Nova senha de acesso (opcional, entre 8 e 20 caracteres com requisitos de complexidade)
 * @param photoUrl  Nova URL da foto de perfil do funcionário (opcional, até 500 caracteres)
 */
@Schema(description = "Dados para atualização parcial do funcionário da empresa integradora")
public record CompanyEmployeeUpdateDTO(

        @Schema(description = "Novo nome completo do funcionário", example = "João da Silva")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String name,

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
        String password,

        @Schema(description = "Nova URL da foto de perfil do colaborador", example = "https://storage.ourosapp.com/profiles/1.jpg")
        @Size(max = 500, message = "A URL da foto deve ter no máximo 500 caracteres")
        @JsonProperty("photo_url")
        @JsonAlias({"photoUrl", "foto_url", "fotoUrl"})
        String photoUrl
) {

    public CompanyEmployeeUpdateDTO(String email, String telephone, String password) {
        this(null, email, telephone, password, null);
    }

    /**
     * Construtor compacto para sanitização de espaços em branco e normalização do e-mail.
     */
    public CompanyEmployeeUpdateDTO {
        name = name != null ? name.trim() : null;
        email = email != null ? email.trim().toLowerCase() : null;
        telephone = telephone != null ? telephone.trim() : null;
        photoUrl = photoUrl != null ? photoUrl.trim() : null;
    }

    /**
     * Verifica se pelo menos um dos campos foi informado para atualização.
     *
     * @return {@code true} se houver pelo menos um campo não nulo e não vazio (ou photoUrl para limpar)
     */
    public boolean hasUpdates() {
        return (name != null && !name.isBlank())
                || (email != null && !email.isBlank())
                || (telephone != null && !telephone.isBlank())
                || (password != null && !password.isBlank())
                || photoUrl != null;
    }
}
