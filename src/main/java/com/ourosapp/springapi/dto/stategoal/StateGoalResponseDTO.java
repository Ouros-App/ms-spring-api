package com.ourosapp.springapi.dto.stategoal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ourosapp.springapi.entity.StateGoal;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * DTO de resposta contendo as informações detalhadas de uma Meta Estadual.
 *
 * @param id           Identificador único da meta estadual
 * @param title        Título descritivo da meta
 * @param description  Detalhamento ou observações da meta
 * @param type         Tipo ou categoria da meta
 * @param status       Status atual da meta
 * @param targetValue  Valor numérico alvo da meta
 * @param dateCreation Data de início da meta
 * @param dateEnd      Data de término da meta
 * @param idFarm       Identificador único da fazenda vinculada
 * @param region       Região de abrangência da meta
 */
@Schema(description = "Resposta contendo os dados detalhados da meta estadual")
public record StateGoalResponseDTO(

        @Schema(description = "Identificador único da meta estadual", example = "1")
        Long id,

        @Schema(description = "Título da meta estadual", example = "Meta Regional Conversão SP")
        String title,

        @Schema(description = "Descrição detalhada da meta", example = "Meta estabelecida para o estado com base na média regional de produtividade")
        String description,

        @Schema(description = "Tipo ou métrica da meta", example = "FEED_CONVERSION")
        String type,

        @Schema(description = "Status da meta", example = "IN_PROGRESS")
        String status,

        @Schema(description = "Valor numérico alvo da meta", example = "1.6500")
        @JsonProperty("target_value")
        BigDecimal targetValue,

        @Schema(description = "Data de início da vigência da meta", example = "2026-01-01")
        @JsonProperty("date_creation")
        LocalDate dateCreation,

        @Schema(description = "Data de término da vigência da meta", example = "2026-12-31")
        @JsonProperty("date_end")
        LocalDate dateEnd,

        @Schema(description = "Identificador único da fazenda vinculada", example = "1")
        @JsonProperty("id_farm")
        Long idFarm,

        @Schema(description = "Região de abrangência da meta", example = "Sudeste")
        String region
) {
    /**
     * Converte uma entidade {@link StateGoal} e a região correspondente em {@link StateGoalResponseDTO}.
     *
     * @param goal   entidade a ser convertida (não deve ser nula)
     * @param region região associada à fazenda da meta
     * @return DTO correspondente
     * @throws NullPointerException se goal for nulo
     */
    public static StateGoalResponseDTO fromEntity(StateGoal goal, String region) {
        Objects.requireNonNull(goal, "StateGoal não pode ser nulo");
        return new StateGoalResponseDTO(
                goal.getId(),
                goal.getTitle(),
                goal.getDescription(),
                goal.getType(),
                goal.getStatus(),
                goal.getTargetValue(),
                goal.getDateCreation(),
                goal.getDateEnd(),
                goal.getIdFarm(),
                region
        );
    }
}
