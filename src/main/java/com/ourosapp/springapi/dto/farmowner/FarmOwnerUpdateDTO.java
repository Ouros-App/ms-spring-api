package com.ourosapp.springapi.dto.farmowner;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de requisição para atualização parcial de Produtor Rural (PATCH /farm-owners/{id}).
 * Todos os campos são opcionais, permitindo atualizar apenas o que for fornecido.
 *
 * @param email     Novo e-mail de acesso (opcional)
 * @param telephone Novo telefone de contato (opcional, entre 10 e 13 dígitos)
 * @param password  Nova senha de acesso (opcional, entre 8 e 20 caracteres com requisitos de complexidade)
 */
@Schema(description = "Dados para atualização parcial do produtor rural")
public record FarmOwnerUpdateDTO(

        @Schema(description = "Novo e-mail de acesso do produtor rural", example = "sebastiao.novo@fazenda.com.br")
        @Email(message = "Formato de e-mail inválido")
        @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres")
        String email,

        @Schema(description = "Novo telefone de contato (entre 10 e 13 dígitos numéricos)", example = "11999998888")
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
    public FarmOwnerUpdateDTO {
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
