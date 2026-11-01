package com.ourosapp.springapi.dto.stategoal;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Meta criada uma única vez, com status Pendente independente por granja")
public record StateGoalByStateResponseDTO(
        @Schema(description = "ID da meta compartilhada", example = "1") Long id,
        @Schema(description = "UF selecionada", example = "SP") String state,
        @JsonProperty("id_enterprise") @Schema(description = "Empresa dos participantes", example = "1") Long idEnterprise,
        @JsonProperty("participants_count") @Schema(description = "Quantidade efetivamente vinculada", example = "15") int participantsCount
) { }
