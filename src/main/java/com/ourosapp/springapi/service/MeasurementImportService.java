package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.energyregistry.EnergyRegistryResponseDTO;
import com.ourosapp.springapi.dto.measurementimport.MeasurementImportRequestDTO;
import com.ourosapp.springapi.dto.measurementimport.MeasurementImportResponseDTO;
import com.ourosapp.springapi.dto.waterregistry.WaterRegistryResponseDTO;
import com.ourosapp.springapi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MeasurementImportService {
    private final WaterRegistryService waterRegistryService;
    private final EnergyRegistryService energyRegistryService;

    @Transactional
    public MeasurementImportResponseDTO createImport(MeasurementImportRequestDTO request, UserPrincipal principal) {
        List<WaterRegistryResponseDTO> water = request.waterRegistries() == null ? List.of()
                : request.waterRegistries().stream()
                        .map(row -> waterRegistryService.createWaterRegistry(row, principal)).toList();
        List<EnergyRegistryResponseDTO> energy = request.energyRegistries() == null ? List.of()
                : request.energyRegistries().stream()
                        .map(row -> energyRegistryService.createEnergyRegistry(row, principal)).toList();
        return new MeasurementImportResponseDTO(water, energy);
    }
}
