package com.ourosapp.springapi.dto.farm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ourosapp.springapi.entity.FarmSimulation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTO de resposta de um cenário de simulação salvo no PostgreSQL, enriquecido opcionalmente
 * com o impacto de posições e liga no Ranking NoSQL (Redis).
 *
 * @param id               Identificador único do cenário de simulação
 * @param idFarm           Identificador da fazenda associada
 * @param idEnterprise     Identificador da empresa integradora
 * @param idLot            Código identificador do lote simulado
 * @param currentCgi       CGI / Consumo atual da fazenda
 * @param targetCgi        CGI / Consumo alvo simulado
 * @param waterPrice       Preço unitário da água (R$/L)
 * @param energyPrice      Preço unitário da energia (R$/kWh)
 * @param poultryQuantity  Quantidade de aves do lote simulado
 * @param targetEnergyKwh  Consumo alvo de energia elétrica (kWh)
 * @param createdAt        Data/hora de criação do cenário
 * @param updatedAt        Data/hora da última atualização do cenário
 * @param simulatedRanking Resultado da simulação de ganho de posições no ranking Redis
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Dados de resposta de um cenário de simulação de fazenda salvo")
public record FarmSimulationResponseDTO(

        @Schema(description = "Identificador único da simulação", example = "1")
        Long id,

        @Schema(description = "Identificador da fazenda", example = "1")
        @JsonProperty("id_farm")
        Long idFarm,

        @Schema(description = "Identificador da empresa integradora", example = "1")
        @JsonProperty("id_enterprise")
        Long idEnterprise,

        @Schema(description = "Código identificador do lote", example = "LT-2026-042")
        @JsonProperty("id_lot")
        String idLot,

        @Schema(description = "CGI / Consumo atual da fazenda", example = "12000.0")
        @JsonProperty("current_cgi")
        Double currentCgi,

        @Schema(description = "CGI / Consumo alvo simulado", example = "9500.0")
        @JsonProperty("target_cgi")
        Double targetCgi,

        @Schema(description = "Preço unitário da água (R$/L)", example = "0.015")
        @JsonProperty("water_price")
        Double waterPrice,

        @Schema(description = "Preço unitário da energia (R$/kWh)", example = "0.85")
        @JsonProperty("energy_price")
        Double energyPrice,

        @Schema(description = "Quantidade de aves no lote simulado", example = "25000")
        @JsonProperty("poultry_quantity")
        Integer poultryQuantity,

        @Schema(description = "Gasto alvo de energia em kWh", example = "3200.0")
        @JsonProperty("target_energy_kwh")
        Double targetEnergyKwh,

        @Schema(description = "Data e hora de criação")
        @JsonProperty("created_at")
        LocalDateTime createdAt,

        @Schema(description = "Data e hora da última atualização")
        @JsonProperty("updated_at")
        LocalDateTime updatedAt,

        @Schema(description = "Resultado consolidado da simulação no ranking da integradora")
        @JsonProperty("simulated_ranking")
        FarmRankingSimulationResponseDTO simulatedRanking
) {
    public static FarmSimulationResponseDTO fromEntity(FarmSimulation entity, FarmRankingSimulationResponseDTO simulatedRanking) {
        return new FarmSimulationResponseDTO(
                entity.getId(),
                entity.getIdFarm(),
                entity.getIdEnterprise(),
                entity.getIdLot(),
                entity.getCurrentCgi(),
                entity.getTargetCgi(),
                entity.getWaterPrice(),
                entity.getEnergyPrice(),
                entity.getPoultryQuantity(),
                entity.getTargetEnergyKwh(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                simulatedRanking
        );
    }
}
