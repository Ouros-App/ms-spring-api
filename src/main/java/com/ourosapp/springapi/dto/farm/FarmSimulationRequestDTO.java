package com.ourosapp.springapi.dto.farm;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para criação de um cenário de simulação financeira e operacional de fazenda/granja.
 *
 * @param idFarm          Identificador da fazenda associada à simulação
 * @param idLot           Código identificador do lote simulado (ex: LT-2026-042)
 * @param currentCgi      Consumo/CGI atual da fazenda
 * @param targetCgi       Consumo/CGI alvo simulado
 * @param waterPrice      Preço unitário da água (R$/L)
 * @param energyPrice     Preço unitário da energia (R$/kWh)
 * @param poultryQuantity Quantidade de aves do lote simulado
 * @param targetEnergyKwh Consumo alvo de energia elétrica (kWh)
 */
@Schema(description = "Dados de entrada para criação de um cenário de simulação de fazenda")
public record FarmSimulationRequestDTO(

        @Schema(description = "Identificador da fazenda", example = "1")
        @NotNull(message = "A fazenda (id_farm) é obrigatória")
        @JsonProperty("id_farm")
        @JsonAlias({"idFarm", "granjaId"})
        Long idFarm,

        @Schema(description = "Código ou identificador do lote", example = "LT-2026-042")
        @NotBlank(message = "O lote (id_lot) é obrigatório")
        @Size(max = 50, message = "O identificador do lote deve ter no máximo 50 caracteres")
        @JsonProperty("id_lot")
        @JsonAlias({"idLot", "lote"})
        String idLot,

        @Schema(description = "CGI / Consumo atual da fazenda", example = "12000.0")
        @NotNull(message = "O CGI atual (current_cgi) é obrigatório")
        @Positive(message = "O CGI atual (current_cgi) deve ser positivo")
        @JsonProperty("current_cgi")
        @JsonAlias({"currentCgi", "consumoAtual"})
        Double currentCgi,

        @Schema(description = "CGI / Consumo alvo simulado", example = "9500.0")
        @NotNull(message = "O CGI alvo (target_cgi) é obrigatório")
        @Positive(message = "O CGI alvo (target_cgi) deve ser positivo")
        @JsonProperty("target_cgi")
        @JsonAlias({"targetCgi", "consumoAlvo"})
        Double targetCgi,

        @Schema(description = "Preço unitário da água (R$/L)", example = "0.015")
        @NotNull(message = "O preço da água (water_price) é obrigatório")
        @Positive(message = "O preço da água (water_price) deve ser positivo")
        @JsonProperty("water_price")
        @JsonAlias({"waterPrice", "precoAgua"})
        Double waterPrice,

        @Schema(description = "Preço unitário da energia (R$/kWh)", example = "0.85")
        @NotNull(message = "O preço da energia (energy_price) é obrigatório")
        @Positive(message = "O preço da energia (energy_price) deve ser positivo")
        @JsonProperty("energy_price")
        @JsonAlias({"energyPrice", "precoEnergia"})
        Double energyPrice,

        @Schema(description = "Quantidade de aves no lote simulado", example = "25000")
        @NotNull(message = "A quantidade de aves (poultry_quantity) é obrigatória")
        @Positive(message = "A quantidade de aves (poultry_quantity) deve ser positiva")
        @JsonProperty("poultry_quantity")
        @JsonAlias({"poultryQuantity", "quantidadeAves"})
        Integer poultryQuantity,

        @Schema(description = "Gasto alvo de energia em kWh", example = "3200.0")
        @NotNull(message = "O gasto alvo de energia (target_energy_kwh) é obrigatório")
        @Positive(message = "O gasto alvo de energia (target_energy_kwh) deve ser positivo")
        @JsonProperty("target_energy_kwh")
        @JsonAlias({"targetEnergyKwh", "gastoAlvoEnergia"})
        Double targetEnergyKwh
) {
}
