package com.ourosapp.springapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ourosapp.springapi.entity.Address;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * DTO de resposta contendo as informações completas de um endereço cadastrado.
 *
 * @param id      Identificador único do endereço
 * @param zipCode CEP do endereço (serializado como zip_code)
 * @param state   Unidade Federativa / Estado com 2 caracteres
 * @param city    Nome da cidade
 * @param number  Número do imóvel
 * @param country Código do país com 2 caracteres
 */
@Schema(description = "Resposta contendo os dados do endereço")
public record AddressResponseDTO(

        @Schema(description = "Identificador do endereço", example = "1")
        Long id,

        @Schema(description = "CEP do endereço", example = "12345678")
        @JsonProperty("zip_code")
        String zipCode,

        @Schema(description = "Estado do endereço", example = "SP")
        String state,

        @Schema(description = "Cidade do endereço", example = "Campinas")
        String city,

        @Schema(description = "Número do endereço", example = "555")
        String number,

        @Schema(description = "País do endereço", example = "BR")
        String country
) {
    /**
     * Converte uma entidade {@link Address} em sua representação {@link AddressResponseDTO}.
     *
     * @param address entidade a ser convertida (não deve ser nula)
     * @return DTO correspondente
     * @throws NullPointerException se address for nulo
     */
    public static AddressResponseDTO fromEntity(Address address) {
        Objects.requireNonNull(address, "Address must not be null");
        return new AddressResponseDTO(
                address.getId(),
                address.getZipCode(),
                address.getState(),
                address.getCity(),
                address.getNumber(),
                address.getCountry()
        );
    }
}
