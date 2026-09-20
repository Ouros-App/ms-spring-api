package com.ourosapp.springapi.dto.individualgoal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ourosapp.springapi.entity.IndividualGoal;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * DTO de resposta contendo as informações de uma Meta Individual de fazenda.
 *
 * @param id          Identificador único da meta individual
 * @param title       Título descritivo da meta
 * @param description Detalhamento ou observações da meta
 * @param type        Tipo ou categoria da meta
 * @param status      Status atual da meta
 * @param targetValue Valor numérico alvo da meta
 * @param idFarm      Identificador único da fazenda vinculada
 */
@Schema(description = "Resposta contendo os dados detalhados da meta individual")
public record IndividualGoalResponseDTO(

        @Schema(description = "Identificador único da meta", example = "1")
        Long id,

        @Schema(description = "Título da meta individual", example = "Reduzir consumo de energia no Galpão 1")
        String title,

        @Schema(description = "Descrição detalhada da meta", example = "Meta operacional para otimizar os ciclos de ventilação noturna")
        String description,

        @Schema(description = "Tipo ou métrica da meta", example = "ENERGY_CONSUMPTION")
        String type,

        @Schema(description = "Status da meta", example = "IN_PROGRESS")
        String status,

        @Schema(description = "Valor numérico alvo da meta", example = "420.5000")
        @JsonProperty("target_value")
        BigDecimal targetValue,

        @Schema(description = "Identificador único da fazenda vinculada", example = "1")
        @JsonProperty("id_farm")
        Long idFarm
) {
    /**
     * Converte uma entidade {@link IndividualGoal} em {@link IndividualGoalResponseDTO}.
     *
     * @param goal entidade a ser convertida (não deve ser nula)
     * @return DTO correspondente
     * @throws NullPointerException se goal for nulo
     */
    public static IndividualGoalResponseDTO fromEntity(IndividualGoal goal) {
        Objects.requireNonNull(goal, "IndividualGoal não pode ser nulo");
        return new IndividualGoalResponseDTO(
                goal.getId(),
                goal.getTitle(),
                goal.getDescription(),
                goal.getType(),
                goal.getStatus(),
                goal.getTargetValue(),
                goal.getIdFarm()
        );
    }
}
