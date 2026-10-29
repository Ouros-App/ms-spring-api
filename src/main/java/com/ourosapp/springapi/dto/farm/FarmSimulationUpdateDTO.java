package com.ourosapp.springapi.dto.farm;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para atualização parcial ({@code PATCH /farms/simulations/{id}}) de um cenário de simulação.
 *
 * @param idFarm          Identificador da fazenda associada à simulação (opcional)
 * @param idLot           Código identificador do lote simulado (opcional)
 * @param currentCgi      Consumo/CGI atual da fazenda (opcional)
 * @param targetCgi       Consumo/CGI alvo simulado (opcional)
 * @param waterPrice      Preço unitário da água (opcional)
 * @param energyPrice     Preço unitário da energia (opcional)
 * @param poultryQuantity Quantidade de aves do lote simulado (opcional)
 * @param targetEnergyKwh Consumo alvo de energia elétrica (opcional)
 */
@Schema(description = "Dados de entrada para atualização parcial de um cenário de simulação de fazenda")
public record FarmSimulationUpdateDTO(

        @Schema(description = "Identificador da fazenda", example = "1")
        @JsonProperty("id_farm")
        @JsonAlias({"idFarm", "granjaId"})
        Long idFarm,

        @Schema(description = "Código ou identificador do lote", example = "LT-2026-042")
        @Size(max = 50, message = "O identificador do lote deve ter no máximo 50 caracteres")
        @JsonProperty("id_lot")
        @JsonAlias({"idLot", "lote"})
        String idLot,

        @Schema(description = "CGI / Consumo atual da fazenda", example = "12000.0")
        @Positive(message = "O CGI atual (current_cgi) deve ser positivo")
        @JsonProperty("current_cgi")
        @JsonAlias({"currentCgi", "consumoAtual"})
        Double currentCgi,

        @Schema(description = "CGI / Consumo alvo simulado", example = "9500.0")
        @Positive(message = "O CGI alvo (target_cgi) deve ser positivo")
        @JsonProperty("target_cgi")
        @JsonAlias({"targetCgi", "consumoAlvo"})
        Double targetCgi,

        @Schema(description = "Preço unitário da água (R$/L)", example = "0.015")
        @Positive(message = "O preço da água (water_price) deve ser positivo")
        @JsonProperty("water_price")
        @JsonAlias({"waterPrice", "precoAgua"})
        Double waterPrice,

        @Schema(description = "Preço unitário da energia (R$/kWh)", example = "0.85")
        @Positive(message = "O preço da energia (energy_price) deve ser positivo")
        @JsonProperty("energy_price")
        @JsonAlias({"energyPrice", "precoEnergia"})
        Double energyPrice,

        @Schema(description = "Quantidade de aves no lote simulado", example = "25000")
        @Positive(message = "A quantidade de aves (poultry_quantity) deve ser positiva")
        @JsonProperty("poultry_quantity")
        @JsonAlias({"poultryQuantity", "quantidadeAves"})
        Integer poultryQuantity,

        @Schema(description = "Gasto alvo de energia em kWh", example = "3200.0")
        @Positive(message = "O gasto alvo de energia (target_energy_kwh) deve ser positivo")
        @JsonProperty("target_energy_kwh")
        @JsonAlias({"targetEnergyKwh", "gastoAlvoEnergia"})
        Double targetEnergyKwh
) {
    /**
     * Verifica se ao menos um campo foi informado para atualização parcial.
     */
    public boolean hasUpdates() {
        return idFarm != null
                || (idLot != null && !idLot.isBlank())
                || currentCgi != null
                || targetCgi != null
                || waterPrice != null
                || energyPrice != null
                || poultryQuantity != null
                || targetEnergyKwh != null;
    }
}
