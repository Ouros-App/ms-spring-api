package com.ourosapp.springapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.dto.energyregistry.EnergyRegistryRequestDTO;
import com.ourosapp.springapi.dto.measurementimport.MeasurementImportRequestDTO;
import com.ourosapp.springapi.dto.waterregistry.WaterRegistryRequestDTO;
import com.ourosapp.springapi.entity.Farm;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.repository.EnergyRegistryRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.repository.FarmRepository;
import com.ourosapp.springapi.repository.WaterRegistryRepository;
import com.ourosapp.springapi.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MeasurementImportPersistenceTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MeasurementImportService measurementImportService;
    @Autowired
    private FarmRepository farmRepository;
    @Autowired
    private FarmOwnerRepository farmOwnerRepository;
    @Autowired
    private WaterRegistryRepository waterRegistryRepository;
    @Autowired
    private EnergyRegistryRepository energyRegistryRepository;

    private Long farmId;
    private Long ownerId;

    @AfterEach
    void cleanUp() {
        if (farmId == null) return;
        waterRegistryRepository.deleteAll(
                waterRegistryRepository.findAllByIdFarmOrderByRegistrationDateDesc(farmId));
        energyRegistryRepository.deleteAll(
                energyRegistryRepository.findByIdFarmOrderByRegistrationDateDesc(farmId));
        if (ownerId != null) {
            farmOwnerRepository.findById(ownerId).ifPresent(farmOwnerRepository::delete);
        }
        farmRepository.findById(farmId).ifPresent(farmRepository::delete);
    }

    @Test
    void httpEndpointPersistsBothMeasurementTypesToTheAuthenticatedOwnersFarm() throws Exception {
        UserPrincipal owner = createOwner();
        MeasurementImportRequestDTO request = new MeasurementImportRequestDTO(
                List.of(water(LocalDate.of(2025, 3, 1), "10.0", "12.5")),
                List.of(energy(LocalDate.of(2025, 3, 1), "7.12")));

        mockMvc.perform(post("/measurement-imports")
                        .with(user(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.water_registries[0].registration_date").value("2025-03-01"))
                .andExpect(jsonPath("$.water_registries[0].id_farm").value(farmId))
                .andExpect(jsonPath("$.energy_registries[0].registration_date").value("2025-03-01"))
                .andExpect(jsonPath("$.energy_registries[0].id_farm").value(farmId));

        assertEquals(1, waterRegistryRepository.findAllByIdFarmOrderByRegistrationDateDesc(farmId).size());
        assertEquals(1, energyRegistryRepository.findByIdFarmOrderByRegistrationDateDesc(farmId).size());
        BigDecimal storedEnergy = energyRegistryRepository
                .findByIdFarmOrderByRegistrationDateDesc(farmId)
                .get(0)
                .getEnergyConsumption();
        assertEquals(0, storedEnergy.compareTo(new BigDecimal("7.12")));
    }

    @Test
    void rejectsEnergyValueThatWouldBeRoundedByTheDatabase() throws Exception {
        UserPrincipal owner = createOwner();
        MeasurementImportRequestDTO request = new MeasurementImportRequestDTO(
                List.of(water(LocalDate.of(2025, 3, 1), "10.0", "12.5")),
                List.of(energy(LocalDate.of(2025, 3, 1), "7.125")));

        mockMvc.perform(post("/measurement-imports")
                        .with(user(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertTrue(waterRegistryRepository.findAllByIdFarmOrderByRegistrationDateDesc(farmId).isEmpty());
        assertTrue(energyRegistryRepository.findByIdFarmOrderByRegistrationDateDesc(farmId).isEmpty());
    }


    @Test
    void rollsBackEarlierRowsWhenALaterRowInTheBatchIsInvalid() {
        UserPrincipal owner = createOwner();
        MeasurementImportRequestDTO request = new MeasurementImportRequestDTO(
                List.of(
                        water(LocalDate.of(2025, 3, 1), "10.0", "12.5"),
                        water(LocalDate.of(2025, 3, 2), "15.0", "14.0")),
                List.of(energy(LocalDate.of(2025, 3, 1), "7.5")));

        assertThrows(ResponseStatusException.class,
                () -> measurementImportService.createImport(request, owner));

        assertTrue(waterRegistryRepository.findAllByIdFarmOrderByRegistrationDateDesc(farmId).isEmpty());
        assertTrue(energyRegistryRepository.findByIdFarmOrderByRegistrationDateDesc(farmId).isEmpty());
    }

    private UserPrincipal createOwner() {
        Farm farm = farmRepository.save(Farm.builder()
                .name("Fazenda Importação")
                .areaProperty(BigDecimal.ONE)
                .region("SP")
                .idAddress(1L)
                .idEnterprise(1L)
                .build());
        farmId = farm.getId();
        FarmOwner owner = farmOwnerRepository.save(FarmOwner.builder()
                .name("Produtor de teste")
                .password("hash")
                .email("import-" + UUID.randomUUID().toString().substring(0, 8) + "@test.local")
                .idFarm(farmId)
                .build());
        ownerId = owner.getId();
        return UserPrincipal.create(owner);
    }

    private static WaterRegistryRequestDTO water(LocalDate date, String start, String end) {
        return new WaterRegistryRequestDTO(date, new BigDecimal(start), new BigDecimal(end), null);
    }

    private static EnergyRegistryRequestDTO energy(LocalDate date, String consumption) {
        return new EnergyRegistryRequestDTO(date, new BigDecimal(consumption), null);
    }
}
