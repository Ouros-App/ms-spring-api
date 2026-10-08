package com.ourosapp.springapi.dto.farm;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de resposta para apresentação da classificação de uma granja/fazenda no Ranking NoSQL (Redis).
 *
 * @param farmId          Identificador único da fazenda
 * @param rankPosition    Posição ordinal da fazenda no ranking (1º, 2º, 3º...)
 * @param score           Pontuação calculada da fazenda no ranking
 * @param farmName        Nome da fazenda
 * @param region          Região de localização da fazenda
 * @param poultryCapacity Capacidade de alojamento de aves
 * @param chickensNow     Quantidade atual de aves alojadas
 */
@Schema(description = "Resposta contendo os dados de classificação da Granja no Ranking Redis NoSQL")
public record FarmRankingResponseDTO(

        @Schema(description = "Identificador único da fazenda", example = "1")
        @JsonProperty("farm_id")
        @JsonAlias("farmId")
        Long farmId,

        @Schema(description = "Posição ordinal no ranking (1-based)", example = "1")
        @JsonProperty("rank_position")
        @JsonAlias("rankPosition")
        Long rankPosition,

        @Schema(description = "Pontuação consolidada da fazenda no ranking", example = "98.75")
        @JsonProperty("score")
        Double score,

        @Schema(description = "Nome da fazenda", example = "Granja São José")
        @JsonProperty("farm_name")
        @JsonAlias("farmName")
        String farmName,

        @Schema(description = "Região da fazenda", example = "Sudeste")
        @JsonProperty("region")
        String region,

        @Schema(description = "Capacidade de alojamento de aves", example = "50000")
        @JsonProperty("poultry_capacity")
        @JsonAlias("poultryCapacity")
        Integer poultryCapacity,

        @Schema(description = "Quantidade atual de aves alojadas", example = "48500")
        @JsonProperty("chickens_now")
        @JsonAlias("chickensNow")
        Integer chickensNow
) {}
