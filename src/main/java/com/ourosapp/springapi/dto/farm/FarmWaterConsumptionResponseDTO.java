package com.ourosapp.springapi.dto.farm;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * DTO de resposta contendo o consumo recente de água calculado para uma fazenda.
 *
 * @param idFarm           identificador único da fazenda
 * @param waterConsumption consumo recente de água registrado pelo hidrômetro
 */
@Schema(description = "DTO de resposta para o consumo recente de água da fazenda")
public record FarmWaterConsumptionResponseDTO(
        @JsonProperty("id_farm")
        @JsonAlias("idFarm")
        @Schema(description = "Identificador único da fazenda", example = "1")
        Long idFarm,

        @JsonProperty("water_consumption")
        @JsonAlias("waterConsumption")
        @Schema(description = "Consumo recente de água baseado no hidrômetro", example = "150.50")
        BigDecimal waterConsumption
) {}
