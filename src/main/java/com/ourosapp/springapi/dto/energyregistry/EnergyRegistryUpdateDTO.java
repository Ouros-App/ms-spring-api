package com.ourosapp.springapi.dto.energyregistry;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de requisição para atualização parcial de um Registro de Consumo de Energia Elétrica (PATCH /energy-registries/{id}).
 *
 * @param registrationDate Nova data da leitura/registro do consumo de energia (opcional)
 * @param energyConsumption Novo valor do consumo de energia (opcional, deve ser maior que zero)
 */
@Schema(description = "Dados para atualização parcial do registro de consumo de energia elétrica")
public record EnergyRegistryUpdateDTO(

        @Schema(description = "Data do registro de consumo de energia", example = "2026-09-10")
        @JsonProperty("registration_date")
        @JsonAlias("registrationDate")
        LocalDate registrationDate,

        @Schema(description = "Consumo de energia registrado (deve ser maior que zero)", example = "480.00")
        @JsonProperty("energy_consumption")
        @JsonAlias("energyConsumption")
        @Positive(message = "O consumo de energia deve ser maior que zero")
        BigDecimal energyConsumption
) {
    /**
     * Verifica se ao menos um dos campos opcionais foi informado para atualização.
     *
     * @return {@code true} se houver pelo menos um campo preenchido, {@code false} caso contrário
     */
    public boolean hasUpdates() {
        return registrationDate != null || energyConsumption != null;
    }
}
