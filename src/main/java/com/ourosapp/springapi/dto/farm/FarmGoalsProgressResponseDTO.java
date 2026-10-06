package com.ourosapp.springapi.dto.farm;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * DTO de resposta para o progresso consolidado de metas de uma fazenda.
 *
 * @param idFarm                  identificador único da fazenda
 * @param goalsProgressPercentage percentual ponderado de metas concluídas/em andamento (0 a 100%)
 */
@Schema(description = "DTO de resposta para o progresso consolidado de metas da fazenda")
public record FarmGoalsProgressResponseDTO(
        @JsonProperty("id_farm")
        @JsonAlias("idFarm")
        @Schema(description = "Identificador único da fazenda", example = "1")
        Long idFarm,

        @JsonProperty("goals_progress_percentage")
        @JsonAlias("goalsProgressPercentage")
        @Schema(description = "Percentual ponderado de progresso das metas (0 a 100%)", example = "75.00")
        BigDecimal goalsProgressPercentage
) {}
