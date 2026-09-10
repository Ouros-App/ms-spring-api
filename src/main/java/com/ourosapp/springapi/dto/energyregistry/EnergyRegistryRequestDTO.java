package com.ourosapp.springapi.dto.energyregistry;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de requisição para cadastro de Registro de Consumo de Energia (POST /energy-registries).
 *
 * @param registrationDate Data da leitura/registro do consumo de energia
 * @param energyConsumption Valor do consumo de energia (maior que zero)
 * @param idFarm            Identificador único da fazenda vinculada
 */
@Schema(description = "Dados para registro de consumo de energia elétrica")
public record EnergyRegistryRequestDTO(

        @Schema(description = "Data do registro de consumo de energia", example = "2026-09-09")
        @JsonProperty("registration_date")
        @JsonAlias("registrationDate")
        @NotNull(message = "A data de registro é obrigatória")
        @PastOrPresent(message = "A data de registro não pode ser uma data futura")
        LocalDate registrationDate,

        @Schema(description = "Consumo de energia registrado (deve ser maior que zero)", example = "450.75")
        @JsonProperty("energy_consumption")
        @JsonAlias("energyConsumption")
        @NotNull(message = "O consumo de energia é obrigatório")
        @Positive(message = "O consumo de energia deve ser maior que zero")
        BigDecimal energyConsumption,

        @Schema(description = "Identificador único da fazenda vinculada (obrigatório para administradores e funcionários; opcional para produtor rural)", example = "1")
        @JsonProperty("id_farm")
        @JsonAlias("idFarm")
        @Positive(message = "O ID da fazenda deve ser maior que zero")
        Long idFarm
) {
}
