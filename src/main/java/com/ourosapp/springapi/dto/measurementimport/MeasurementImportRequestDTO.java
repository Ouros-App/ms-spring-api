package com.ourosapp.springapi.dto.measurementimport;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ourosapp.springapi.dto.energyregistry.EnergyRegistryRequestDTO;
import com.ourosapp.springapi.dto.waterregistry.WaterRegistryRequestDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Lote de registros de consumo de água e energia")
public record MeasurementImportRequestDTO(
        @Schema(description = "Registros de medição de água")
        @JsonProperty("water_registries") @JsonAlias("waterRegistries")
        @Size(max = 500, message = "O lote pode conter no máximo 500 registros")
        List<@NotNull @Valid WaterRegistryRequestDTO> waterRegistries,

        @Schema(description = "Registros de consumo de energia")
        @JsonProperty("energy_registries") @JsonAlias("energyRegistries")
        @Size(max = 500, message = "O lote pode conter no máximo 500 registros")
        List<@NotNull @Valid EnergyRegistryRequestDTO> energyRegistries
) {
    @AssertTrue(message = "Informe ao menos um registro e no máximo 500 registros no total")
    @Schema(hidden = true)
    public boolean hasValidBatchSize() {
        int water = waterRegistries == null ? 0 : waterRegistries.size();
        int energy = energyRegistries == null ? 0 : energyRegistries.size();
        return water + energy > 0 && water + energy <= 500;
    }
}
