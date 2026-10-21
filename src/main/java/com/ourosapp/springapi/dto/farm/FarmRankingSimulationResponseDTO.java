package com.ourosapp.springapi.dto.farm;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de resposta para a simulação de ganho de posições e divisão de uma granja no Ranking NoSQL (Redis).
 *
 * @param farmId                 Identificador único da fazenda
 * @param farmName               Nome da fazenda
 * @param currentScore           Pontuação atual consolidada da fazenda no ranking (CGI)
 * @param currentPosition        Posição ordinal atual da fazenda no ranking (1-based)
 * @param currentLeague          Divisão/liga atual da fazenda (ex: OURO, PRATA, BRONZE, COBRE, FERRO)
 * @param simulatedScore         Pontuação hipotética simulada informada na requisição
 * @param simulatedPosition      Posição ordinal simulada calculada com a nova pontuação
 * @param simulatedLeague        Nova divisão/liga alcançada na simulação
 * @param positionsGained        Quantidade de posições conquistadas (delta positivo ou 0)
 * @param totalFarms             Quantidade total de fazendas participantes do ranking na integradora
 * @param simulatedPercentilePct Percentil relativo simulado alcançado no ranking (0.0 a 100.0)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Resposta contendo os resultados da simulação de ganho de posições no ranking da integradora")
public record FarmRankingSimulationResponseDTO(

        @Schema(description = "Identificador único da fazenda", example = "1")
        @JsonProperty("farm_id")
        @JsonAlias("farmId")
        Long farmId,

        @Schema(description = "Nome da fazenda", example = "Granja São Judas Tadeu")
        @JsonProperty("farm_name")
        @JsonAlias("farmName")
        String farmName,

        @Schema(description = "Pontuação atual consolidada no ranking", example = "7.85")
        @JsonProperty("current_score")
        @JsonAlias("currentScore")
        Double currentScore,

        @Schema(description = "Posição ordinal atual no ranking (1-based)", example = "12")
        @JsonProperty("current_position")
        @JsonAlias("currentPosition")
        Long currentPosition,

        @Schema(description = "Divisão/liga atual da fazenda", example = "BRONZE")
        @JsonProperty("current_league")
        @JsonAlias("currentLeague")
        String currentLeague,

        @Schema(description = "Pontuação hipotética simulada", example = "6.07")
        @JsonProperty("simulated_score")
        @JsonAlias("simulatedScore")
        Double simulatedScore,

        @Schema(description = "Posição ordinal simulada calculada (1-based)", example = "4")
        @JsonProperty("simulated_position")
        @JsonAlias("simulatedPosition")
        Long simulatedPosition,

        @Schema(description = "Nova divisão/liga alcançada na simulação", example = "OURO")
        @JsonProperty("simulated_league")
        @JsonAlias("simulatedLeague")
        String simulatedLeague,

        @Schema(description = "Quantidade de posições conquistadas na simulação", example = "8")
        @JsonProperty("positions_gained")
        @JsonAlias("positionsGained")
        Long positionsGained,

        @Schema(description = "Total de fazendas ativas no ranking da integradora", example = "25")
        @JsonProperty("total_farms")
        @JsonAlias("totalFarms")
        Long totalFarms,

        @Schema(description = "Percentil relativo simulado alcançado no ranking", example = "16.0")
        @JsonProperty("simulated_percentile_pct")
        @JsonAlias("simulatedPercentilePct")
        Double simulatedPercentilePct
) {
    public FarmRankingSimulationResponseDTO {
        if (farmName != null) {
            farmName = farmName.trim();
        }
        if (currentLeague != null) {
            currentLeague = currentLeague.trim().toUpperCase();
        }
        if (simulatedLeague != null) {
            simulatedLeague = simulatedLeague.trim().toUpperCase();
        }
        if (positionsGained == null || positionsGained < 0) {
            positionsGained = 0L;
        }
    }
}
