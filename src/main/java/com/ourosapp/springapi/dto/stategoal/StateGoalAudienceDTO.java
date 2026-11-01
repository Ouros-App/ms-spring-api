package com.ourosapp.springapi.dto.stategoal;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Quantidade de granjas elegíveis na UF e empresa; recalculada na criação")
public record StateGoalAudienceDTO(
        @Schema(description = "UF brasileira", example = "SP") String state,
        @JsonProperty("id_enterprise") @Schema(description = "Empresa do público", example = "1") Long idEnterprise,
        @JsonProperty("participants_count") @Schema(description = "Quantidade de granjas participantes", example = "15") int participantsCount
) { }
