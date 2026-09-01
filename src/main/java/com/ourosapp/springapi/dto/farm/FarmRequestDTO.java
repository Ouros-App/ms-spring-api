package com.ourosapp.springapi.dto.farm;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ourosapp.springapi.dto.AddressRequestDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO de requisição para cadastro de Fazenda (POST /farms).
 * Permite informar um endereço já existente via {@code id_address} OU cadastrar um novo endereço embutido via {@code address}.
 *
 * @param name            Nome da fazenda
 * @param areaProperty    Área total da propriedade (maior que zero)
 * @param region          Região de localização da fazenda
 * @param poultryCapacity Capacidade de aves alojadas (não negativa)
 * @param place           Localização ou identificação do sítio/granja
 * @param idAddress       Identificador do endereço cadastrado (opcional se {@code address} for informado)
 * @param address         Dados para cadastro de novo endereço na mesma requisição (opcional se {@code id_address} for informado)
 * @param idEnterprise    Identificador da empresa integradora vinculada
 */
@Schema(description = "Dados para cadastro de uma nova fazenda")
public record FarmRequestDTO(

        @Schema(description = "Nome da fazenda", example = "Fazenda Santa Maria")
        @NotBlank(message = "O nome da fazenda não pode estar em branco")
        @Size(max = 100, message = "O nome da fazenda deve ter no máximo 100 caracteres")
        String name,

        @Schema(description = "Área da propriedade em hectares ou metros quadrados", example = "150.50")
        @JsonProperty("area_property")
        @JsonAlias("areaProperty")
        @NotNull(message = "A área da propriedade é obrigatória")
        @Positive(message = "A área da propriedade deve ser maior que zero")
        BigDecimal areaProperty,

        @Schema(description = "Região da fazenda", example = "Sudeste")
        @NotBlank(message = "A região não pode estar em branco")
        @Size(max = 50, message = "A região deve ter no máximo 50 caracteres")
        String region,

        @Schema(description = "Capacidade de alojamento de aves", example = "50000")
        @JsonProperty("poultry_capacity")
        @JsonAlias("poultryCapacity")
        @NotNull(message = "A capacidade de aves é obrigatória")
        @Min(value = 0, message = "A capacidade de aves não pode ser negativa")
        Integer poultryCapacity,

        @Schema(description = "Local ou denominação do sítio/granja", example = "Gleba 4 - Setor Sul")
        @NotBlank(message = "O local não pode estar em branco")
        @Size(max = 50, message = "O local deve ter no máximo 50 caracteres")
        String place,

        @Schema(description = "Identificador de um endereço pré-existente (opcional se o objeto 'address' for informado)", example = "1")
        @JsonProperty("id_address")
        @JsonAlias("idAddress")
        @Positive(message = "O ID do endereço deve ser maior que zero")
        Long idAddress,

        @Schema(description = "Dados para cadastro de novo endereço embutido na mesma requisição (opcional se 'id_address' for informado)")
        @JsonProperty("address")
        @Valid
        AddressRequestDTO address,

        @Schema(description = "Identificador da empresa integradora vinculada", example = "1")
        @JsonProperty("id_enterprise")
        @JsonAlias("idEnterprise")
        @NotNull(message = "O ID da empresa integradora é obrigatório")
        @Positive(message = "O ID da empresa integradora deve ser maior que zero")
        Long idEnterprise
) {
    /**
     * Construtor compacto para sanitização automática de espaços em branco.
     */
    public FarmRequestDTO {
        name = name != null ? name.trim() : null;
        region = region != null ? region.trim() : null;
        place = place != null ? place.trim() : null;
    }

    /**
     * Validação cruzada para garantir que exatamente uma forma de endereço seja informada
     * (ou idAddress existente ou objeto de novo endereço address, mas nunca ambos nem nenhum).
     *
     * @return {@code true} se exatamente uma das opções de endereço estiver presente
     */
    @AssertTrue(message = "É obrigatório informar exatamente uma forma de endereço: 'id_address' ou o objeto 'address' completo, mas não ambos nem nenhum")
    public boolean hasValidAddressInfo() {
        return (idAddress != null) ^ (address != null);
    }
}
