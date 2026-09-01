package com.ourosapp.springapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ourosapp.springapi.entity.Enterprise;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * DTO de resposta contendo as informações completas da Empresa Integradora.
 *
 * @param id             Identificador único da empresa
 * @param name           Nome da empresa
 * @param email          E-mail de contato corporativo
 * @param documentNumber CNPJ / Documento da empresa (serializado como document_number)
 * @param telephone      Telefone de contato
 * @param idAddress      Identificador do endereço vinculado (serializado como id_address)
 */
@Schema(description = "Resposta contendo os dados da Empresa Integradora")
public record EnterpriseResponseDTO(

        @Schema(description = "Identificador único da empresa", example = "1")
        Long id,

        @Schema(description = "Nome da empresa", example = "Agro Ouros S.A.")
        String name,

        @Schema(description = "E-mail de contato corporativo", example = "contato@agroouros.com.br")
        String email,

        @Schema(description = "CNPJ / Documento da empresa", example = "12345678000195")
        @JsonProperty("document_number")
        String documentNumber,

        @Schema(description = "Telefone de contato", example = "11999999999")
        String telephone,

        @Schema(description = "Identificador do endereço vinculado", example = "1")
        @JsonProperty("id_address")
        Long idAddress
) {
    /**
     * Converte uma entidade {@link Enterprise} em {@link EnterpriseResponseDTO}.
     *
     * @param enterprise entidade a ser convertida (não deve ser nula)
     * @return DTO correspondente
     * @throws NullPointerException se enterprise for nula
     */
    public static EnterpriseResponseDTO fromEntity(Enterprise enterprise) {
        Objects.requireNonNull(enterprise, "Enterprise não pode ser nulo");
        return new EnterpriseResponseDTO(
                enterprise.getId(),
                enterprise.getName(),
                enterprise.getEmail(),
                enterprise.getDocumentNumber(),
                enterprise.getTelephone(),
                enterprise.getIdAddress()
        );
    }
}