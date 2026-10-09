package com.ourosapp.springapi.dto.farm;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO de resposta para a visualização relativa da fazenda no ranking (Pódio).
 * Apresenta a fazenda solicitante, seus concorrentes diretos e informações sobre o próximo reset.
 *
 * @param currentFarmId         Identificador da fazenda solicitante
 * @param rankingPodium         Lista contendo o trio de concorrência direta (ABOVE, CURRENT, BELOW)
 * @param secondsUntilNextReset Segundos restantes até o próximo reset do ranking
 * @param timeUntilNextReset    Tempo restante legível em português até o próximo reset
 * @param nextResetAt           Data/hora ISO-8601 exata do próximo reset
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Resposta contendo a visualização de pódio relativo da fazenda no ranking")
public record FarmPodiumRankingResponseDTO(

        @Schema(description = "Identificador único da fazenda consultada", example = "10")
        @JsonProperty("current_farm_id")
        @JsonAlias("currentFarmId")
        Long currentFarmId,

        @Schema(description = "Pódio relativo com os concorrentes diretos na classificação")
        @JsonProperty("ranking_podium")
        @JsonAlias("rankingPodium")
        List<FarmPodiumItemDTO> rankingPodium,

        @Schema(description = "Tempo restante em segundos até o próximo reset do ranking", example = "216000")
        @JsonProperty("seconds_until_next_reset")
        @JsonAlias("secondsUntilNextReset")
        Long secondsUntilNextReset,

        @Schema(description = "Tempo restante formatado de forma legível em português até o próximo reset", example = "2 dias, 12 horas e 30 minutos")
        @JsonProperty("time_until_next_reset")
        @JsonAlias("timeUntilNextReset")
        String timeUntilNextReset,

        @Schema(description = "Data e hora exatas do próximo reset em formato ISO-8601", example = "2026-10-11T00:00:00-03:00")
        @JsonProperty("next_reset_at")
        @JsonAlias("nextResetAt")
        String nextResetAt
) {
    public FarmPodiumRankingResponseDTO {
        rankingPodium = rankingPodium != null ? List.copyOf(rankingPodium) : List.of();
    }

    public FarmPodiumRankingResponseDTO(Long currentFarmId, List<FarmPodiumItemDTO> rankingPodium) {
        this(currentFarmId, rankingPodium, null, null, null);
    }

    public FarmPodiumRankingResponseDTO withCountdown(RankingCountdownDTO countdown) {
        if (countdown == null) {
            return this;
        }
        return new FarmPodiumRankingResponseDTO(
                currentFarmId, rankingPodium,
                countdown.secondsUntilNextReset(),
                countdown.timeUntilNextReset(),
                countdown.nextResetAt()
        );
    }
}
