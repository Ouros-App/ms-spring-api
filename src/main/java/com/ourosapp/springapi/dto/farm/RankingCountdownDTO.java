package com.ourosapp.springapi.dto.farm;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Dados de contagem regressiva para o próximo recálculo/reset do ranking de fazendas.
 *
 * @param secondsUntilNextReset tempo restante em segundos
 * @param time                  tempo restante estruturado em objeto JSON com horas, minutos e dias para exibição mobile
 * @param nextResetAt           data/hora ISO-8601 exata do próximo recálculo (ex: "2026-10-11T00:00:00-03:00")
 */
@Schema(description = "Informações sobre o tempo restante até o próximo recálculo/reset do ranking")
public record RankingCountdownDTO(

        @Schema(description = "Tempo restante em segundos até o próximo reset", example = "216000")
        @JsonProperty("seconds_until_next_reset")
        @JsonAlias("secondsUntilNextReset")
        Long secondsUntilNextReset,

        @Schema(description = "Tempo restante estruturado em dias, horas e minutos para exibição mobile")
        @JsonProperty("time")
        @JsonAlias({"time_until_next_reset", "timeUntilNextReset"})
        RankingTimeDTO time,

        @Schema(description = "Data e hora exatas do próximo reset em formato ISO-8601", example = "2026-10-11T00:00:00-03:00")
        @JsonProperty("next_reset_at")
        @JsonAlias("nextResetAt")
        String nextResetAt
) {}
