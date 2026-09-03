package com.ourosapp.springapi.dto.enterprise;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ourosapp.springapi.dto.address.AddressRequestDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.br.CNPJ;

/**
 * DTO de requisição para atualização parcial de Empresa Integradora (PATCH).
 * Todos os campos são opcionais, permitindo atualizar apenas o que for fornecido.
 */
@Schema(description = "Dados para atualização parcial da Empresa Integradora")
public record EnterpriseUpdateDTO(

        @Schema(description = "Novo nome da empresa", example = "Agro Ouros S.A.")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String name,

        @Schema(description = "Novo e-mail de contato corporativo", example = "contato@agroouros.com.br")
        @Email(message = "Formato de e-mail inválido")
        @Size(max = 50, message = "O e-mail deve ter no máximo 50 caracteres")
        String email,

        @Schema(description = "Novo CNPJ / Documento da empresa (14 dígitos numéricos)", example = "12345678000195")
        @JsonProperty("document_number")
        @JsonAlias("documentNumber")
        @CNPJ(message = "O documento/CNPJ deve ser válido")
        String documentNumber,

        @Schema(description = "Novo telefone de contato (apenas números, entre 10 e 13 dígitos)", example = "11999999999")
        @Pattern(regexp = "^$|^\\d{10,13}$", message = "O telefone deve conter apenas números e ter entre 10 e 13 dígitos")
        String telephone,

        @Schema(description = "Novo identificador do endereço cadastrado", example = "2")
        @JsonProperty("id_address")
        @JsonAlias("idAddress")
        @Positive(message = "O ID do endereço deve ser maior que zero")
        Long idAddress
) {
    /**
     * Construtor compacto para sanitização automática.
     */
    public EnterpriseUpdateDTO {
        name = name != null ? name.trim() : null;
        email = email != null ? email.trim().toLowerCase() : null;
        documentNumber = documentNumber != null ? documentNumber.trim() : null;
        telephone = telephone != null ? telephone.trim() : null;
    }

    /**
     * Verifica se pelo menos um dos campos foi informado para atualização.
     *
     * @return {@code true} se houver pelo menos um campo preenchido
     */
    public boolean hasUpdates() {
        return (name != null && !name.isBlank())
                || (email != null && !email.isBlank())
                || (documentNumber != null && !documentNumber.isBlank())
                || (telephone != null && !telephone.isBlank())
                || idAddress != null;
    }
}
