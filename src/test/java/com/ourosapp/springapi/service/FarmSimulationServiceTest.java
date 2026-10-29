package com.ourosapp.springapi.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ourosapp.springapi.dto.farm.FarmRankingSimulationResponseDTO;
import com.ourosapp.springapi.dto.farm.FarmSimulationRequestDTO;
import com.ourosapp.springapi.dto.farm.FarmSimulationResponseDTO;
import com.ourosapp.springapi.dto.farm.FarmSimulationUpdateDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.Farm;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.entity.FarmSimulation;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.repository.FarmRepository;
import com.ourosapp.springapi.repository.FarmSimulationRepository;
import com.ourosapp.springapi.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários do FarmSimulationService")
class FarmSimulationServiceTest {

    @Mock
    private FarmSimulationRepository farmSimulationRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private CompanyEmployeeRepository companyEmployeeRepository;

    @Mock
    private FarmOwnerRepository farmOwnerRepository;

    @Mock
    private FarmRankingService farmRankingService;

    @InjectMocks
    private FarmSimulationService farmSimulationService;

    private UserPrincipal admPrincipal;
    private UserPrincipal employeePrincipal;
    private UserPrincipal ownerPrincipal;
    private Farm sampleFarm;
    private CompanyEmployee sampleEmployee;
    private FarmOwner sampleOwner;
    private FarmSimulation sampleSimulation;
    private FarmRankingSimulationResponseDTO sampleRankingSim;

    @BeforeEach
    void setUp() {
        admPrincipal = new UserPrincipal(
                1L, "adm@ouros.com", "pass", "ADM",
                List.of(new SimpleGrantedAuthority("ROLE_ADM"))
        );
        employeePrincipal = new UserPrincipal(
                10L, "func@integradora.com", "pass", "COMPANY_EMPLOYEE",
                List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE"))
        );
        ownerPrincipal = new UserPrincipal(
                20L, "produtor@granja.com", "pass", "FARM_OWNER",
                List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER"))
        );

        sampleFarm = Farm.builder()
                .id(1L)
                .name("Granja Sol Nascente")
                .areaProperty(new BigDecimal("100.0"))
                .poultryCapacity(25000)
                .region("Sudoeste")
                .idEnterprise(5L)
                .idAddress(1L)
                .build();

        sampleEmployee = CompanyEmployee.builder()
                .id(10L)
                .name("Carlos")
                .email("func@integradora.com")
                .password("hash")
                .idEnterprise(5L)
                .build();

        sampleOwner = FarmOwner.builder()
                .id(20L)
                .name("João")
                .email("produtor@granja.com")
                .password("hash")
                .idFarm(1L)
                .build();

        sampleSimulation = new FarmSimulation(
                100L,
                1L,
                5L,
                "LT-2026-042",
                12000.0,
                9500.0,
                0.015,
                0.85,
                25000,
                3200.0,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        sampleRankingSim = new FarmRankingSimulationResponseDTO(
                1L,
                "Granja Sol Nascente",
                12000.0,
                5L,
                "BRONZE",
                9500.0,
                2L,
                "OURO",
                3L,
                10L,
                20.0
        );
    }

    @Test
    @DisplayName("Deve listar simulações da empresa para COMPANY_EMPLOYEE")
    void getSimulations_companyEmployee_success() {
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(sampleEmployee));
        when(farmSimulationRepository.findByIdEnterpriseOrderByIdDesc(5L)).thenReturn(List.of(sampleSimulation));
        when(farmRankingService.simulateFarmRanking(eq(1L), eq(9500.0), any())).thenReturn(sampleRankingSim);

        List<FarmSimulationResponseDTO> result = farmSimulationService.getSimulations(null, null, employeePrincipal);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).id());
        assertEquals("LT-2026-042", result.get(0).idLot());
        assertNotNull(result.get(0).simulatedRanking());
        assertEquals("OURO", result.get(0).simulatedRanking().simulatedLeague());
    }

    @Test
    @DisplayName("Deve bloquear COMPANY_EMPLOYEE ao consultar simulações de outra empresa")
    void getSimulations_companyEmployeeOtherEnterprise_throws403() {
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(sampleEmployee));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> farmSimulationService.getSimulations(null, 99L, employeePrincipal));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve listar simulações para ADM com filtros")
    void getSimulations_adm_success() {
        when(farmSimulationRepository.findByIdEnterpriseAndIdFarmOrderByIdDesc(5L, 1L))
                .thenReturn(List.of(sampleSimulation));
        when(farmRankingService.simulateFarmRanking(eq(1L), eq(9500.0), any())).thenReturn(sampleRankingSim);

        List<FarmSimulationResponseDTO> result = farmSimulationService.getSimulations(1L, 5L, admPrincipal);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).idFarm());
    }

    @Test
    @DisplayName("Deve listar simulações para FARM_OWNER da própria fazenda")
    void getSimulations_farmOwner_success() {
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(sampleOwner));
        when(farmSimulationRepository.findByIdFarmOrderByIdDesc(1L)).thenReturn(List.of(sampleSimulation));
        when(farmRankingService.simulateFarmRanking(eq(1L), eq(9500.0), any())).thenReturn(sampleRankingSim);

        List<FarmSimulationResponseDTO> result = farmSimulationService.getSimulations(null, null, ownerPrincipal);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).id());
    }

    @Test
    @DisplayName("Deve criar simulação com sucesso para COMPANY_EMPLOYEE")
    void createSimulation_success() {
        FarmSimulationRequestDTO request = new FarmSimulationRequestDTO(
                1L, "LT-2026-042", 12000.0, 9500.0, 0.015, 0.85, 25000, 3200.0
        );
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(sampleEmployee));
        when(farmSimulationRepository.save(any(FarmSimulation.class))).thenReturn(sampleSimulation);
        when(farmRankingService.simulateFarmRanking(eq(1L), eq(9500.0), any())).thenReturn(sampleRankingSim);

        FarmSimulationResponseDTO created = farmSimulationService.createSimulation(request, employeePrincipal);

        assertNotNull(created);
        assertEquals(100L, created.id());
        assertEquals(5L, created.idEnterprise());
        assertEquals(9500.0, created.targetCgi());
        verify(farmSimulationRepository).save(any(FarmSimulation.class));
    }

    @Test
    @DisplayName("Deve atualizar simulação existente com sucesso")
    void updateSimulation_success() {
        FarmSimulationUpdateDTO updateDTO = new FarmSimulationUpdateDTO(
                null, "LT-2026-099", 11000.0, 8800.0, 0.018, 0.90, 26000, 3000.0
        );
        when(farmSimulationRepository.findById(100L)).thenReturn(Optional.of(sampleSimulation));
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(sampleEmployee));
        when(farmSimulationRepository.save(any(FarmSimulation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(farmRankingService.simulateFarmRanking(eq(1L), eq(8800.0), any())).thenReturn(sampleRankingSim);

        FarmSimulationResponseDTO updated = farmSimulationService.updateSimulation(100L, updateDTO, employeePrincipal);

        assertEquals("LT-2026-099", updated.idLot());
        assertEquals(8800.0, updated.targetCgi());
        assertEquals(26000, updated.poultryQuantity());
    }

    @Test
    @DisplayName("Deve lançar 400 Bad Request ao atualizar simulação sem campos informados")
    void updateSimulation_emptyPayload_throws400() {
        FarmSimulationUpdateDTO emptyDTO = new FarmSimulationUpdateDTO(
                null, null, null, null, null, null, null, null
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> farmSimulationService.updateSimulation(100L, emptyDTO, employeePrincipal));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve excluir simulação existente com sucesso")
    void deleteSimulation_success() {
        when(farmSimulationRepository.findById(100L)).thenReturn(Optional.of(sampleSimulation));
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(sampleEmployee));

        farmSimulationService.deleteSimulation(100L, employeePrincipal);

        verify(farmSimulationRepository).delete(sampleSimulation);
    }

    @Test
    @DisplayName("Deve bloquear exclusão de simulação de outra empresa")
    void deleteSimulation_otherEnterprise_throws403() {
        CompanyEmployee otherEmployee = CompanyEmployee.builder()
                .id(10L).name("Carlos").email("func@outra.com").password("hash").idEnterprise(99L).build();
        when(farmSimulationRepository.findById(100L)).thenReturn(Optional.of(sampleSimulation));
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(otherEmployee));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> farmSimulationService.deleteSimulation(100L, employeePrincipal));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }
}
