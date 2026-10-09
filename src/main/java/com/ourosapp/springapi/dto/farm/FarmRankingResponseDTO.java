package com.ourosapp.springapi.dto.farm;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ourosapp.springapi.repository.projection.FarmRankingProjection;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO simplificado de resposta para apresentação da classificação de uma granja/fazenda no Ranking NoSQL (Redis).
 * Retorna estritamente o identificador, o nome, a região, o valor (score), a posição no ranking e o tempo até o próximo reset.
 *
 * @param farmId                Identificador único da fazenda
 * @param farmName              Nome da fazenda
 * @param region                Região de localização da fazenda
 * @param score                 Pontuação calculada da fazenda no ranking (CGI)
 * @param rankPosition          Posição ordinal da fazenda no ranking (1º, 2º, 3º...)
 * @param secondsUntilNextReset Segundos restantes até o próximo reset do ranking
 * @param timeUntilNextReset    Tempo restante legível em português até o próximo reset
 * @param nextResetAt           Data/hora ISO-8601 exata do próximo reset
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Resposta contendo os dados essenciais de classificação da Granja no Ranking Redis NoSQL")
public record FarmRankingResponseDTO(

        @Schema(description = "Identificador único da fazenda", example = "1")
        @JsonProperty("farm_id")
        @JsonAlias("farmId")
        Long farmId,

        @Schema(description = "Nome da fazenda", example = "Granja São José")
        @JsonProperty("farm_name")
        @JsonAlias("farmName")
        String farmName,

        @Schema(description = "Região da fazenda", example = "Sudeste")
        @JsonProperty("region")
        String region,

        @Schema(description = "Pontuação consolidada da fazenda no ranking", example = "1.2345")
        @JsonProperty("score")
        Double score,

        @Schema(description = "Posição ordinal no ranking (1-based)", example = "1")
        @JsonProperty("rank_position")
        @JsonAlias("rankPosition")
        Long rankPosition,

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

    public FarmRankingResponseDTO {
        farmName = farmName != null ? farmName.trim() : null;
    }

    public FarmRankingResponseDTO(Long farmId, String farmName, String region, Double score, Long rankPosition) {
        this(farmId, farmName, region, score, rankPosition, null, null, null);
    }

    public FarmRankingResponseDTO withCountdown(RankingCountdownDTO countdown) {
        if (countdown == null) {
            return this;
        }
        return new FarmRankingResponseDTO(
                farmId, farmName, region, score, rankPosition,
                countdown.secondsUntilNextReset(),
                countdown.timeUntilNextReset(),
                countdown.nextResetAt()
        );
    }

    /**
     * Cria um FarmRankingResponseDTO a partir de uma projeção analítica do PostgreSQL.
     *
     * @param projection projeção com métricas calculadas
     * @return DTO simplificado preenchido
     */
    public static FarmRankingResponseDTO fromProjection(FarmRankingProjection projection) {
        return new FarmRankingResponseDTO(
                projection.getFarmId(),
                projection.getFarmName() != null ? projection.getFarmName().trim() : null,
                projection.getRegion(),
                projection.getCgi(),
                projection.getRankPosition()
        );
    }
}
