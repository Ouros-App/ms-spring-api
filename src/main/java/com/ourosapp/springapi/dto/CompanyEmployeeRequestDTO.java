package com.ourosapp.springapi.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de requisição para cadastro de Funcionário da Empresa Integradora.
 */
@Schema(description = "Dados para cadastro do funcionário da empresa integradora")
public record CompanyEmployeeRequestDTO(

        @Schema(description = "Nome completo do funcionário", example = "João da Silva")
        @NotBlank(message = "O nome não pode estar em branco")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String name,

        @Schema(description = "Documento/CPF do funcionário (exatamente 11 dígitos numéricos)", example = "12345678901")
        @JsonProperty("document_number")
        @JsonAlias("documentNumber")
        @NotBlank(message = "O documento/CPF não pode estar em branco")
        @Pattern(regexp = "^\\d{11}$", message = "O documento deve conter exatamente 11 dígitos numéricos")
        String documentNumber,

        @Schema(description = "E-mail corporativo do funcionário", example = "joao.silva@empresa.com.br")
        @NotBlank(message = "O e-mail não pode estar em branco")
        @Email(message = "Formato de e-mail inválido")
        @Size(max = 50, message = "O e-mail deve ter no máximo 50 caracteres")
        String email,

        @Schema(description = "Telefone de contato (entre 10 e 13 dígitos numéricos)", example = "11987654321")
        @NotBlank(message = "O telefone não pode estar em branco")
        @Pattern(regexp = "^\\d{10,13}$", message = "O telefone deve conter apenas números e ter entre 10 e 13 dígitos")
        String telephone,

        @Schema(description = "Senha de acesso do funcionário", example = "SenhaSegura@123")
        @NotBlank(message = "A senha não pode estar em branco")
        @Size(min = 6, max = 100, message = "A senha deve ter entre 6 e 100 caracteres")
        String password,

        @Schema(description = "Identificador da empresa integradora vinculada", example = "1")
        @JsonProperty("id_enterprise")
        @JsonAlias("idEnterprise")
        @NotNull(message = "O ID da empresa integradora é obrigatório")
        Long idEnterprise
) {
    /**
     * Construtor compacto para sanitização automática de espaços em branco e normalização de e-mail.
     */
    public CompanyEmployeeRequestDTO {
        name = name != null ? name.trim() : null;
        documentNumber = documentNumber != null ? documentNumber.trim() : null;
        email = email != null ? email.trim().toLowerCase() : null;
        telephone = telephone != null ? telephone.trim() : null;
    }
}