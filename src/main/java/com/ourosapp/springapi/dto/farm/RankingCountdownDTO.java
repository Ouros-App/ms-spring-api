package com.ourosapp.springapi.dto.farm;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Dados de contagem regressiva para o próximo recálculo/reset do ranking de fazendas.
 *
 * @param secondsUntilNextReset tempo restante em segundos
 * @param timeUntilNextReset    tempo restante formatado em texto legível (ex: "2 dias, 12 horas e 30 minutos")
 * @param nextResetAt           data/hora ISO-8601 exata do próximo recálculo (ex: "2026-10-11T00:00:00-03:00")
 */
@Schema(description = "Informações sobre o tempo restante até o próximo recálculo/reset do ranking")
public record RankingCountdownDTO(

        @Schema(description = "Tempo restante em segundos até o próximo reset", example = "216000")
        @JsonProperty("seconds_until_next_reset")
        @JsonAlias("secondsUntilNextReset")
        Long secondsUntilNextReset,

        @Schema(description = "Tempo restante formatado de forma legível em português", example = "2 dias, 12 horas e 30 minutos")
        @JsonProperty("time_until_next_reset")
        @JsonAlias("timeUntilNextReset")
        String timeUntilNextReset,

        @Schema(description = "Data e hora exatas do próximo reset em formato ISO-8601", example = "2026-10-11T00:00:00-03:00")
        @JsonProperty("next_reset_at")
        @JsonAlias("nextResetAt")
        String nextResetAt
) {}
