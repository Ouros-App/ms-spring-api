package com.ourosapp.springapi.dto.farm;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Item do pódio de ranqueamento relativo da fazenda contendo dados essenciais.
 *
 * @param farmId       Identificador único da fazenda
 * @param farmName     Nome da fazenda (sem cidade)
 * @param region       Região de localização da fazenda
 * @param score        Pontuação calculada da fazenda no ranking (CGI)
 * @param rankPosition Posição ordinal no ranking da integradora
 * @param relation     Relação de vizinhança na classificação (ABOVE, CURRENT, BELOW)
 */
@Schema(description = "Item de classificação relativa no pódio de ranqueamento da fazenda")
public record FarmPodiumItemDTO(

        @Schema(description = "Identificador único da fazenda", example = "10")
        @JsonProperty("farm_id")
        @JsonAlias("farmId")
        Long farmId,

        @Schema(description = "Nome da fazenda (sem cidade)", example = "Granja São José")
        @JsonProperty("farm_name")
        @JsonAlias("farmName")
        String farmName,

        @Schema(description = "Região da fazenda", example = "Sudeste")
        @JsonProperty("region")
        String region,

        @Schema(description = "Pontuação consolidada da fazenda no ranking", example = "1.2345")
        @JsonProperty("score")
        Double score,

        @Schema(description = "Posição ordinal no ranking da integradora (1-based)", example = "5")
        @JsonProperty("rank_position")
        @JsonAlias("rankPosition")
        Long rankPosition,

        @Schema(description = "Relação de concorrência direta no ranking (ABOVE, CURRENT, BELOW)", example = "CURRENT")
        @JsonProperty("relation")
        String relation
) {
    public FarmPodiumItemDTO {
        farmName = farmName != null ? farmName.trim() : null;
    }
}
