package com.ourosapp.springapi.dto.farmowner;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

/**
 * DTO de requisição para cadastro de Produtor Rural (POST /farm-owners).
 *
 * @param name           Nome completo do produtor rural
 * @param documentNumber Documento/CPF do produtor (11 dígitos numéricos ou formatado)
 * @param email          E-mail de acesso do produtor
 * @param telephone      Telefone de contato (entre 10 e 13 dígitos numéricos)
 * @param password       Senha de acesso (8 a 20 caracteres com requisitos de complexidade)
 * @param idFarm         Identificador da fazenda vinculada
 */
@Schema(description = "Dados para cadastro de um novo produtor rural vinculado a uma fazenda")
public record FarmOwnerRequestDTO(

        @Schema(description = "Nome completo do produtor rural", example = "Sebastião da Silva")
        @NotBlank(message = "O nome não pode estar em branco")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String name,

        @Schema(description = "Documento/CPF do produtor rural (11 dígitos numéricos ou formatado)", example = "12345678901")
        @JsonProperty("document_number")
        @JsonAlias({"documentNumber", "zip_code", "zipCode"})
        @NotBlank(message = "O documento/CPF não pode estar em branco")
        @CPF(message = "O documento/CPF deve ser válido")
        String documentNumber,

        @Schema(description = "E-mail de acesso do produtor rural", example = "sebastiao.silva@fazenda.com.br")
        @NotBlank(message = "O e-mail não pode estar em branco")
        @Email(message = "Formato de e-mail inválido")
        @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres")
        String email,

        @Schema(description = "Telefone de contato (entre 10 e 13 dígitos numéricos)", example = "11987654321")
        @NotBlank(message = "O telefone não pode estar em branco")
        @Pattern(regexp = "^\\d{10,13}$", message = "O telefone deve conter apenas números e ter entre 10 e 13 dígitos")
        String telephone,

        @Schema(description = "Senha de acesso do produtor rural", example = "SenhaSegura@123")
        @NotBlank(message = "A senha não pode estar em branco")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,20}$",
                message = "A senha deve ter entre 8 e 20 caracteres, incluindo pelo menos uma letra maiúscula, uma minúscula, um número e um caractere especial"
        )
        String password,

        @Schema(description = "Identificador da fazenda vinculada", example = "1")
        @JsonProperty("id_farm")
        @JsonAlias("idFarm")
        @NotNull(message = "O ID da fazenda é obrigatório")
        @Positive(message = "O ID da fazenda deve ser maior que zero")
        Long idFarm
) {
    /**
     * Construtor compacto para sanitização automática de espaços em branco e normalização de e-mail e CPF.
     */
    public FarmOwnerRequestDTO {
        name = name != null ? name.trim() : null;
        documentNumber = documentNumber != null ? documentNumber.trim().replaceAll("[-.]", "") : null;
        email = email != null ? email.trim().toLowerCase() : null;
        telephone = telephone != null ? telephone.trim() : null;
    }
}
