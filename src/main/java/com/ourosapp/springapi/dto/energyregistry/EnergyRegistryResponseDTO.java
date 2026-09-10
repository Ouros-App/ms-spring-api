package com.ourosapp.springapi.dto.energyregistry;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ourosapp.springapi.entity.EnergyRegistry;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * DTO de resposta contendo as informações de um Registro de Consumo de Energia Elétrica.
 *
 * @param id                Identificador único do registro
 * @param registrationDate Data do registro de consumo
 * @param energyConsumption Valor do consumo de energia
 * @param idFarm            Identificador único da fazenda vinculada
 */
@Schema(description = "Resposta contendo os dados do registro de consumo de energia")
public record EnergyRegistryResponseDTO(

        @Schema(description = "Identificador único do registro", example = "1")
        Long id,

        @Schema(description = "Data do registro de consumo", example = "2026-09-09")
        @JsonProperty("registration_date")
        LocalDate registrationDate,

        @Schema(description = "Consumo de energia registrado", example = "450.75")
        @JsonProperty("energy_consumption")
        BigDecimal energyConsumption,

        @Schema(description = "Identificador único da fazenda vinculada", example = "1")
        @JsonProperty("id_farm")
        Long idFarm
) {
    /**
     * Converte uma entidade {@link EnergyRegistry} em {@link EnergyRegistryResponseDTO}.
     *
     * @param registry entidade a ser convertida (não deve ser nula)
     * @return DTO correspondente
     * @throws NullPointerException se registry for nulo
     */
    public static EnergyRegistryResponseDTO fromEntity(EnergyRegistry registry) {
        Objects.requireNonNull(registry, "EnergyRegistry não pode ser nulo");
        return new EnergyRegistryResponseDTO(
                registry.getId(),
                registry.getRegistrationDate(),
                registry.getEnergyConsumption(),
                registry.getIdFarm()
        );
    }
}
