package com.ourosapp.springapi.dto.measurementimport;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ourosapp.springapi.dto.energyregistry.EnergyRegistryResponseDTO;
import com.ourosapp.springapi.dto.waterregistry.WaterRegistryResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Registros criados no lote de importação")
public record MeasurementImportResponseDTO(
        @JsonProperty("water_registries") @JsonAlias("waterRegistries")
        List<WaterRegistryResponseDTO> waterRegistries,
        @JsonProperty("energy_registries") @JsonAlias("energyRegistries")
        List<EnergyRegistryResponseDTO> energyRegistries
) {
}
