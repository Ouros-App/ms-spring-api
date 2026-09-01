package com.ourosapp.springapi.dto.farm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ourosapp.springapi.entity.Farm;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * DTO de resposta contendo as informações completas de uma Fazenda.
 *
 * @param id              Identificador único da fazenda
 * @param name            Nome da fazenda
 * @param areaProperty    Área total da propriedade
 * @param region          Região de localização da fazenda
 * @param poultryCapacity Capacidade de alojamento de aves
 * @param place           Localização ou denominação do sítio/granja
 * @param idAddress       Identificador do endereço vinculado
 * @param idEnterprise    Identificador da empresa integradora vinculada
 */
@Schema(description = "Resposta contendo os dados da Fazenda")
public record FarmResponseDTO(

        @Schema(description = "Identificador único da fazenda", example = "1")
        Long id,

        @Schema(description = "Nome da fazenda", example = "Fazenda Santa Maria")
        String name,

        @Schema(description = "Área da propriedade", example = "150.50")
        @JsonProperty("area_property")
        BigDecimal areaProperty,

        @Schema(description = "Região da fazenda", example = "Sudeste")
        String region,

        @Schema(description = "Capacidade de alojamento de aves", example = "50000")
        @JsonProperty("poultry_capacity")
        Integer poultryCapacity,

        @Schema(description = "Local ou denominação do sítio/granja", example = "Gleba 4 - Setor Sul")
        String place,

        @Schema(description = "Identificador do endereço vinculado", example = "1")
        @JsonProperty("id_address")
        Long idAddress,

        @Schema(description = "Identificador da empresa integradora vinculada", example = "1")
        @JsonProperty("id_enterprise")
        Long idEnterprise
) {
    /**
     * Converte uma entidade {@link Farm} em {@link FarmResponseDTO}.
     *
     * @param farm entidade a ser convertida (não deve ser nula)
     * @return DTO correspondente
     * @throws NullPointerException se farm for nula
     */
    public static FarmResponseDTO fromEntity(Farm farm) {
        Objects.requireNonNull(farm, "Farm não pode ser nulo");
        return new FarmResponseDTO(
                farm.getId(),
                farm.getName(),
                farm.getAreaProperty(),
                farm.getRegion(),
                farm.getPoultryCapacity(),
                farm.getPlace(),
                farm.getIdAddress(),
                farm.getIdEnterprise()
        );
    }
}
