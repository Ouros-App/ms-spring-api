package com.ourosapp.springapi.dto.enterprise;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ourosapp.springapi.dto.address.AddressRequestDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CNPJ;

/**
 * DTO de requisição para cadastro e atualização de Empresa Integradora.
 */
@Schema(description = "Dados para criação e atualização da Empresa Integradora")
public record EnterpriseRequestDTO(

        @Schema(description = "Nome da empresa", example = "Agro Ouros S.A.")
        @NotBlank(message = "O nome da empresa não pode estar em branco")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String name,

        @Schema(description = "E-mail de contato corporativo", example = "contato@agroouros.com.br")
        @NotBlank(message = "O e-mail não pode estar em branco")
        @Email(message = "Formato de e-mail inválido")
        @Size(max = 50, message = "O e-mail deve ter no máximo 50 caracteres")
        String email,

        @Schema(description = "CNPJ / Documento da empresa (14 dígitos numéricos)", example = "12345678000195")
        @JsonProperty("document_number")
        @JsonAlias("documentNumber")
        @NotBlank(message = "O CNPJ/documento não pode estar em branco")
        @CNPJ(message = "O documento/CNPJ deve ser válido")
        String documentNumber,

        @Schema(description = "Telefone de contato (apenas números, entre 10 e 13 dígitos)", example = "11999999999")
        @NotBlank(message = "O telefone não pode estar em branco")
        @Pattern(regexp = "^\\d{10,13}$", message = "O telefone deve conter apenas números e ter entre 10 e 13 dígitos")
        String telephone,

        @Schema(description = "Identificador do endereço cadastrado", example = "1")
        @JsonProperty("id_address")
        @JsonAlias("idAddress")
        @Positive(message = "O ID do endereço deve ser maior que zero")
        Long idAddress,

        @Schema(description = "Dados do novo endereço (caso não seja fornecido um id_address existente)")
        @Valid
        AddressRequestDTO address
) {
    /**
     * Construtor compacto para sanitização automática (trim, lowercase do e-mail e extração de dígitos do CNPJ).
     */
    public EnterpriseRequestDTO {
        name = name != null ? name.trim() : null;
        email = email != null ? email.trim().toLowerCase() : null;
        documentNumber = documentNumber != null ? documentNumber.trim().replaceAll("[-./]", "") : null;
        telephone = telephone != null ? telephone.trim() : null;
    }

    /**
     * Validação cruzada para garantir que exatamente uma forma de endereço seja informada
     * (ou idAddress existente ou objeto de novo endereço address, mas não ambos nem nenhum).
     *
     * @return {@code true} se exatamente uma das opções de endereço estiver presente
     */
    @Schema(hidden = true)
    @AssertTrue(message = "É obrigatório informar exatamente uma forma de endereço: 'id_address' ou o objeto 'address' completo, mas não ambos nem nenhum")
    public boolean hasValidAddressInfo() {
        return (idAddress != null) ^ (address != null);
    }
}
