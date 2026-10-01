package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.waterregistry.WaterRegistryRequestDTO;
import com.ourosapp.springapi.dto.waterregistry.WaterRegistryResponseDTO;
import com.ourosapp.springapi.dto.waterregistry.WaterRegistryUpdateDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.Farm;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.entity.WaterRegistry;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.repository.FarmRepository;
import com.ourosapp.springapi.repository.WaterRegistryRepository;
import com.ourosapp.springapi.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para a camada de serviço {@link WaterRegistryService}.
 */
@ExtendWith(MockitoExtension.class)
class WaterRegistryServiceTest {

    @Mock
    private WaterRegistryRepository waterRegistryRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private CompanyEmployeeRepository companyEmployeeRepository;

    @Mock
    private FarmOwnerRepository farmOwnerRepository;

    @InjectMocks
    private WaterRegistryService waterRegistryService;

    private Farm sampleFarm;
    private WaterRegistry sampleRegistry;
    private WaterRegistryRequestDTO sampleRequest;
    private UserPrincipal adminPrincipal;
    private UserPrincipal employeePrincipal;
    private UserPrincipal farmOwnerPrincipal;

    @BeforeEach
    void setUp() {
        sampleFarm = Farm.builder()
                .id(1L)
                .name("Fazenda Ouro Verde")
                .areaProperty(new BigDecimal("150.50"))
                .region("Sudeste")
                .poultryCapacity(50000)
                .place("Gleba 4")
                .idAddress(10L)
                .idEnterprise(20L)
                .build();

        sampleRegistry = WaterRegistry.builder()
                .id(100L)
                .registrationDate(LocalDate.of(2026, 9, 10))
                .startHydrometer(new BigDecimal("100.5000"))
                .endHydrometer(new BigDecimal("120.8000"))
                .idFarm(1L)
                .build();

        sampleRequest = new WaterRegistryRequestDTO(
                LocalDate.of(2026, 9, 10),
                new BigDecimal("100.5000"),
                new BigDecimal("120.8000"),
                1L
        );

        adminPrincipal = new UserPrincipal(
                1L, "adm@ouros.com", "pass", "ADM", List.of(new SimpleGrantedAuthority("ROLE_ADM"))
        );

        employeePrincipal = new UserPrincipal(
                10L, "emp@empresa.com", "pass", "COMPANY_EMPLOYEE", List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE"))
        );

        farmOwnerPrincipal = new UserPrincipal(
                20L, "produtor@fazenda.com", "pass", "FARM_OWNER", List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER"))
        );
    }

    // =========================================================================
    // CREATE TESTS
    // =========================================================================

    @Test
    @DisplayName("Deve cadastrar medição de água com sucesso como ADM")
    void testCreateWaterRegistryAsAdmSuccess() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(waterRegistryRepository.save(any(WaterRegistry.class))).thenReturn(sampleRegistry);

        WaterRegistryResponseDTO response = waterRegistryService.createWaterRegistry(sampleRequest, adminPrincipal);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(LocalDate.of(2026, 9, 10), response.registrationDate());
        assertEquals(new BigDecimal("100.5000"), response.startHydrometer());
        assertEquals(new BigDecimal("120.8000"), response.endHydrometer());
        assertEquals(1L, response.idFarm());

        verify(farmRepository).findById(1L);
        verify(waterRegistryRepository).save(any(WaterRegistry.class));
    }

    @Test
    @DisplayName("Deve lançar 400 quando ADM não informar idFarm")
    void testCreateWaterRegistryAsAdmNullFarmId() {
        WaterRegistryRequestDTO requestWithoutFarm = new WaterRegistryRequestDTO(
                LocalDate.of(2026, 9, 10),
                new BigDecimal("100.5000"),
                new BigDecimal("120.8000"),
                null
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                waterRegistryService.createWaterRegistry(requestWithoutFarm, adminPrincipal)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("O ID da fazenda é obrigatório"));
        verify(waterRegistryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve cadastrar medição de água com sucesso como COMPANY_EMPLOYEE da mesma empresa")
    void testCreateWaterRegistryAsCompanyEmployeeSuccess() {
        CompanyEmployee employee = CompanyEmployee.builder().id(10L).idEnterprise(20L).build();
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(waterRegistryRepository.save(any(WaterRegistry.class))).thenReturn(sampleRegistry);

        WaterRegistryResponseDTO response = waterRegistryService.createWaterRegistry(sampleRequest, employeePrincipal);

        assertNotNull(response);
        assertEquals(100L, response.id());
        verify(companyEmployeeRepository).findById(10L);
        verify(waterRegistryRepository).save(any(WaterRegistry.class));
    }

    @Test
    @DisplayName("Deve lançar 403 quando COMPANY_EMPLOYEE tentar cadastrar medição em fazenda de outra empresa")
    void testCreateWaterRegistryAsCompanyEmployeeDifferentEnterpriseForbidden() {
        CompanyEmployee employee = CompanyEmployee.builder().id(10L).idEnterprise(999L).build();
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                waterRegistryService.createWaterRegistry(sampleRequest, employeePrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(waterRegistryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar 400 quando COMPANY_EMPLOYEE não informar idFarm")
    void testCreateWaterRegistryAsCompanyEmployeeNullFarmId() {
        WaterRegistryRequestDTO requestWithoutFarm = new WaterRegistryRequestDTO(
                LocalDate.of(2026, 9, 10),
                new BigDecimal("100.5000"),
                new BigDecimal("120.8000"),
                null
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                waterRegistryService.createWaterRegistry(requestWithoutFarm, employeePrincipal)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(waterRegistryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve cadastrar medição de água com sucesso como FARM_OWNER da própria fazenda")
    void testCreateWaterRegistryAsFarmOwnerSuccess() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(1L).build();
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));
        when(waterRegistryRepository.save(any(WaterRegistry.class))).thenReturn(sampleRegistry);

        WaterRegistryResponseDTO response = waterRegistryService.createWaterRegistry(sampleRequest, farmOwnerPrincipal);

        assertNotNull(response);
        assertEquals(100L, response.id());
        verify(farmOwnerRepository).findById(20L);
        verify(waterRegistryRepository).save(any(WaterRegistry.class));
    }

    @Test
    @DisplayName("Deve cadastrar medição de água como FARM_OWNER inferindo idFarm quando omitido no payload")
    void testCreateWaterRegistryAsFarmOwnerWithoutFarmIdSuccess() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(1L).build();
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));
        when(waterRegistryRepository.save(any(WaterRegistry.class))).thenReturn(sampleRegistry);

        WaterRegistryRequestDTO requestWithoutFarm = new WaterRegistryRequestDTO(
                LocalDate.of(2026, 9, 10),
                new BigDecimal("100.5000"),
                new BigDecimal("120.8000"),
                null
        );

        WaterRegistryResponseDTO response = waterRegistryService.createWaterRegistry(requestWithoutFarm, farmOwnerPrincipal);

        assertNotNull(response);
        assertEquals(100L, response.id());
        verify(waterRegistryRepository).save(argThat(entity -> Long.valueOf(1L).equals(entity.getIdFarm())));
    }

    @Test
    @DisplayName("Deve lançar 403 quando FARM_OWNER tentar cadastrar medição em outra fazenda")
    void testCreateWaterRegistryAsFarmOwnerDifferentFarmForbidden() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(999L).build();
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                waterRegistryService.createWaterRegistry(sampleRequest, farmOwnerPrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(waterRegistryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar 400 quando FARM_OWNER não tiver fazenda vinculada")
    void testCreateWaterRegistryAsFarmOwnerNullLinkedFarm() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(null).build();
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                waterRegistryService.createWaterRegistry(sampleRequest, farmOwnerPrincipal)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(waterRegistryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar 400 quando endHydrometer for menor que startHydrometer")
    void testCreateWaterRegistryInvalidReadingsBadRequest() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));

        WaterRegistryRequestDTO invalidRequest = new WaterRegistryRequestDTO(
                LocalDate.of(2026, 9, 10),
                new BigDecimal("100.00"),
                new BigDecimal("90.00"),
                1L
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                waterRegistryService.createWaterRegistry(invalidRequest, adminPrincipal)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("A leitura final do hidrômetro não pode ser menor que a leitura inicial"));
        verify(waterRegistryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar 401 ao cadastrar medição com principal nulo")
    void testCreateWaterRegistryNullPrincipal() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                waterRegistryService.createWaterRegistry(sampleRequest, null)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 403 para role não autorizada ao cadastrar medição")
    void testCreateWaterRegistryForbiddenRole() {
        UserPrincipal guest = new UserPrincipal(1L, "guest@test.com", "pass", "GUEST", List.of(new SimpleGrantedAuthority("ROLE_GUEST")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                waterRegistryService.createWaterRegistry(sampleRequest, guest)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 409 quando ocorrer DataIntegrityViolationException ao salvar medição")
    void testCreateWaterRegistryDataIntegrityViolationConflict() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(waterRegistryRepository.save(any(WaterRegistry.class)))
                .thenThrow(new DataIntegrityViolationException("Erro de FK"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                waterRegistryService.createWaterRegistry(sampleRequest, adminPrincipal)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // =========================================================================
    // GET ALL / FILTER TESTS
    // =========================================================================

    @Test
    @DisplayName("Deve listar todos os registros de água como ADM sem filtro")
    void testGetWaterRegistriesForAdmAll() {
        when(waterRegistryRepository.findAll()).thenReturn(List.of(sampleRegistry));

        List<WaterRegistryResponseDTO> result = waterRegistryService.getWaterRegistriesForUser(null, adminPrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(waterRegistryRepository).findAll();
    }

    @Test
    @DisplayName("Deve listar registros de água filtrados por fazenda como ADM")
    void testGetWaterRegistriesForAdmFiltered() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(waterRegistryRepository.findAllByIdFarm(1L)).thenReturn(List.of(sampleRegistry));

        List<WaterRegistryResponseDTO> result = waterRegistryService.getWaterRegistriesForUser(1L, adminPrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(farmRepository).findById(1L);
        verify(waterRegistryRepository).findAllByIdFarm(1L);
    }

    @Test
    @DisplayName("Deve listar registros de água de todas as fazendas da empresa para COMPANY_EMPLOYEE")
    void testGetWaterRegistriesForCompanyEmployeeAllFarms() {
        CompanyEmployee employee = CompanyEmployee.builder().id(10L).idEnterprise(20L).build();
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(farmRepository.findAllByIdEnterprise(20L)).thenReturn(List.of(sampleFarm));
        when(waterRegistryRepository.findAllByIdFarmIn(List.of(1L))).thenReturn(List.of(sampleRegistry));

        List<WaterRegistryResponseDTO> result = waterRegistryService.getWaterRegistriesForUser(null, employeePrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(waterRegistryRepository).findAllByIdFarmIn(List.of(1L));
    }

    @Test
    @DisplayName("Deve retornar lista vazia para COMPANY_EMPLOYEE quando empresa não possuir fazendas")
    void testGetWaterRegistriesForCompanyEmployeeNoFarms() {
        CompanyEmployee employee = CompanyEmployee.builder().id(10L).idEnterprise(20L).build();
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(farmRepository.findAllByIdEnterprise(20L)).thenReturn(List.of());

        List<WaterRegistryResponseDTO> result = waterRegistryService.getWaterRegistriesForUser(null, employeePrincipal);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(waterRegistryRepository, never()).findAllByIdFarmIn(any());
    }

    @Test
    @DisplayName("Deve listar registros de fazenda específica para COMPANY_EMPLOYEE da mesma empresa")
    void testGetWaterRegistriesForCompanyEmployeeSpecificFarmSuccess() {
        CompanyEmployee employee = CompanyEmployee.builder().id(10L).idEnterprise(20L).build();
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(waterRegistryRepository.findAllByIdFarm(1L)).thenReturn(List.of(sampleRegistry));

        List<WaterRegistryResponseDTO> result = waterRegistryService.getWaterRegistriesForUser(1L, employeePrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(waterRegistryRepository).findAllByIdFarm(1L);
    }

    @Test
    @DisplayName("Deve lançar 403 para COMPANY_EMPLOYEE ao filtrar registros de fazenda de outra empresa")
    void testGetWaterRegistriesForCompanyEmployeeDifferentEnterpriseForbidden() {
        CompanyEmployee employee = CompanyEmployee.builder().id(10L).idEnterprise(999L).build();
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                waterRegistryService.getWaterRegistriesForUser(1L, employeePrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve listar registros de água da própria fazenda para FARM_OWNER")
    void testGetWaterRegistriesForFarmOwnerSuccess() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(1L).build();
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));
        when(waterRegistryRepository.findAllByIdFarm(1L)).thenReturn(List.of(sampleRegistry));

        List<WaterRegistryResponseDTO> result = waterRegistryService.getWaterRegistriesForUser(null, farmOwnerPrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(waterRegistryRepository).findAllByIdFarm(1L);
    }

    @Test
    @DisplayName("Deve lançar 403 para FARM_OWNER ao tentar filtrar registros de outra fazenda")
    void testGetWaterRegistriesForFarmOwnerDifferentFarmForbidden() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(1L).build();
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                waterRegistryService.getWaterRegistriesForUser(999L, farmOwnerPrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve retornar lista vazia para FARM_OWNER quando não possuir fazenda vinculada")
    void testGetWaterRegistriesForFarmOwnerNullLinkedFarm() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(null).build();
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));

        List<WaterRegistryResponseDTO> result = waterRegistryService.getWaterRegistriesForUser(null, farmOwnerPrincipal);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Deve lançar 403 para role inválida ao listar registros de água")
    void testGetWaterRegistriesForInvalidRoleForbidden() {
        UserPrincipal guest = new UserPrincipal(1L, "guest@test.com", "pass", "GUEST", List.of(new SimpleGrantedAuthority("ROLE_GUEST")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                waterRegistryService.getWaterRegistriesForUser(null, guest)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // =========================================================================
    // GET BY ID TESTS
    // =========================================================================

    @Test
    @DisplayName("Deve buscar registro de água por ID com sucesso como ADM")
    void testGetWaterRegistryByIdAsAdmSuccess() {
        when(waterRegistryRepository.findById(100L)).thenReturn(Optional.of(sampleRegistry));

        WaterRegistryResponseDTO response = waterRegistryService.getWaterRegistryById(100L, adminPrincipal);

        assertNotNull(response);
        assertEquals(100L, response.id());
    }

    @Test
    @DisplayName("Deve buscar registro de água por ID como COMPANY_EMPLOYEE da mesma empresa")
    void testGetWaterRegistryByIdAsCompanyEmployeeSuccess() {
        CompanyEmployee employee = CompanyEmployee.builder().id(10L).idEnterprise(20L).build();
        when(waterRegistryRepository.findById(100L)).thenReturn(Optional.of(sampleRegistry));
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));

        WaterRegistryResponseDTO response = waterRegistryService.getWaterRegistryById(100L, employeePrincipal);

        assertNotNull(response);
        assertEquals(100L, response.id());
    }

    @Test
    @DisplayName("Deve lançar 403 ao buscar registro de água por ID como COMPANY_EMPLOYEE de outra empresa")
    void testGetWaterRegistryByIdAsCompanyEmployeeDifferentEnterpriseForbidden() {
        CompanyEmployee employee = CompanyEmployee.builder().id(10L).idEnterprise(999L).build();
        when(waterRegistryRepository.findById(100L)).thenReturn(Optional.of(sampleRegistry));
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                waterRegistryService.getWaterRegistryById(100L, employeePrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve buscar registro de água por ID como FARM_OWNER da mesma fazenda")
    void testGetWaterRegistryByIdAsFarmOwnerSuccess() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(1L).build();
        when(waterRegistryRepository.findById(100L)).thenReturn(Optional.of(sampleRegistry));
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));

        WaterRegistryResponseDTO response = waterRegistryService.getWaterRegistryById(100L, farmOwnerPrincipal);

        assertNotNull(response);
        assertEquals(100L, response.id());
    }

    @Test
    @DisplayName("Deve lançar 403 ao buscar registro de água por ID como FARM_OWNER de outra fazenda")
    void testGetWaterRegistryByIdAsFarmOwnerDifferentFarmForbidden() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(999L).build();
        when(waterRegistryRepository.findById(100L)).thenReturn(Optional.of(sampleRegistry));
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                waterRegistryService.getWaterRegistryById(100L, farmOwnerPrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 404 ao buscar registro inexistente por ID")
    void testGetWaterRegistryByIdNotFound() {
        when(waterRegistryRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                waterRegistryService.getWaterRegistryById(999L, adminPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 404 ao buscar registro com ID nulo")
    void testGetWaterRegistryByIdNull() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                waterRegistryService.getWaterRegistryById(null, adminPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // =========================================================================
    // UPDATE TESTS
    // =========================================================================

    @Test
    @DisplayName("Deve atualizar parcialmente registro de água com sucesso como ADM")
    void testUpdateWaterRegistryAsAdmSuccess() {
        when(waterRegistryRepository.findById(100L)).thenReturn(Optional.of(sampleRegistry));
        when(waterRegistryRepository.save(any(WaterRegistry.class))).thenAnswer(inv -> inv.getArgument(0));

        WaterRegistryUpdateDTO updateDTO = new WaterRegistryUpdateDTO(
                new BigDecimal("150.0000"),
                null,
                LocalDate.of(2026, 9, 11)
        );

        WaterRegistryResponseDTO response = waterRegistryService.updateWaterRegistry(100L, updateDTO, adminPrincipal);

        assertNotNull(response);
        assertEquals(new BigDecimal("150.0000"), response.endHydrometer());
        assertEquals(LocalDate.of(2026, 9, 11), response.registrationDate());
        verify(waterRegistryRepository).save(any(WaterRegistry.class));
    }

    @Test
    @DisplayName("Deve atualizar parcialmente registro com novo startHydrometer válido")
    void testUpdateWaterRegistryStartHydrometerSuccess() {
        when(waterRegistryRepository.findById(100L)).thenReturn(Optional.of(sampleRegistry));
        when(waterRegistryRepository.save(any(WaterRegistry.class))).thenAnswer(inv -> inv.getArgument(0));

        WaterRegistryUpdateDTO updateDTO = new WaterRegistryUpdateDTO(
                null,
                new BigDecimal("110.0000"),
                null
        );

        WaterRegistryResponseDTO response = waterRegistryService.updateWaterRegistry(100L, updateDTO, adminPrincipal);

        assertNotNull(response);
        assertEquals(new BigDecimal("110.0000"), response.startHydrometer());
        verify(waterRegistryRepository).save(any(WaterRegistry.class));
    }

    @Test
    @DisplayName("Deve retornar registro inalterado quando DTO não tiver atualizações (hasUpdates = false)")
    void testUpdateWaterRegistryNoUpdates() {
        when(waterRegistryRepository.findById(100L)).thenReturn(Optional.of(sampleRegistry));

        WaterRegistryUpdateDTO emptyUpdate = new WaterRegistryUpdateDTO(null, null, null);

        WaterRegistryResponseDTO response = waterRegistryService.updateWaterRegistry(100L, emptyUpdate, adminPrincipal);

        assertNotNull(response);
        assertEquals(100L, response.id());
        verify(waterRegistryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar 400 ao atualizar quando endHydrometer for menor que startHydrometer")
    void testUpdateWaterRegistryInvalidReadings() {
        when(waterRegistryRepository.findById(100L)).thenReturn(Optional.of(sampleRegistry));

        WaterRegistryUpdateDTO invalidUpdate = new WaterRegistryUpdateDTO(
                new BigDecimal("50.0000"), // menor que startHydrometer (100.5000)
                null,
                null
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                waterRegistryService.updateWaterRegistry(100L, invalidUpdate, adminPrincipal)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("A leitura final do hidrômetro não pode ser menor que a leitura inicial"));
        verify(waterRegistryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve atualizar parcialmente registro como FARM_OWNER da mesma fazenda")
    void testUpdateWaterRegistryAsFarmOwnerSuccess() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(1L).build();
        when(waterRegistryRepository.findById(100L)).thenReturn(Optional.of(sampleRegistry));
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));
        when(waterRegistryRepository.save(any(WaterRegistry.class))).thenAnswer(inv -> inv.getArgument(0));

        WaterRegistryUpdateDTO updateDTO = new WaterRegistryUpdateDTO(new BigDecimal("130.0000"), null, null);

        WaterRegistryResponseDTO response = waterRegistryService.updateWaterRegistry(100L, updateDTO, farmOwnerPrincipal);

        assertNotNull(response);
        assertEquals(new BigDecimal("130.0000"), response.endHydrometer());
        verify(waterRegistryRepository).save(any(WaterRegistry.class));
    }

    @Test
    @DisplayName("Deve lançar 403 ao atualizar registro como COMPANY_EMPLOYEE de outra empresa")
    void testUpdateWaterRegistryAsCompanyEmployeeDifferentEnterpriseForbidden() {
        CompanyEmployee employee = CompanyEmployee.builder().id(10L).idEnterprise(999L).build();
        when(waterRegistryRepository.findById(100L)).thenReturn(Optional.of(sampleRegistry));
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));

        WaterRegistryUpdateDTO updateDTO = new WaterRegistryUpdateDTO(new BigDecimal("130.0000"), null, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                waterRegistryService.updateWaterRegistry(100L, updateDTO, employeePrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(waterRegistryRepository, never()).save(any());
    }

    // =========================================================================
    // DELETE TESTS
    // =========================================================================

    @Test
    @DisplayName("Deve remover registro de água com sucesso como ADM")
    void testDeleteWaterRegistryAsAdmSuccess() {
        when(waterRegistryRepository.findById(100L)).thenReturn(Optional.of(sampleRegistry));
        doNothing().when(waterRegistryRepository).delete(sampleRegistry);

        assertDoesNotThrow(() -> waterRegistryService.deleteWaterRegistry(100L, adminPrincipal));

        verify(waterRegistryRepository).delete(sampleRegistry);
        verify(waterRegistryRepository).flush();
    }

    @Test
    @DisplayName("Deve remover registro de água com sucesso como FARM_OWNER da própria fazenda")
    void testDeleteWaterRegistryAsFarmOwnerSuccess() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(1L).build();
        when(waterRegistryRepository.findById(100L)).thenReturn(Optional.of(sampleRegistry));
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));
        doNothing().when(waterRegistryRepository).delete(sampleRegistry);

        assertDoesNotThrow(() -> waterRegistryService.deleteWaterRegistry(100L, farmOwnerPrincipal));

        verify(waterRegistryRepository).delete(sampleRegistry);
    }

    @Test
    @DisplayName("Deve lançar 403 ao remover registro como FARM_OWNER de outra fazenda")
    void testDeleteWaterRegistryAsFarmOwnerDifferentFarmForbidden() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(999L).build();
        when(waterRegistryRepository.findById(100L)).thenReturn(Optional.of(sampleRegistry));
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                waterRegistryService.deleteWaterRegistry(100L, farmOwnerPrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(waterRegistryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve lançar 404 ao remover registro inexistente")
    void testDeleteWaterRegistryNotFound() {
        when(waterRegistryRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                waterRegistryService.deleteWaterRegistry(999L, adminPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(waterRegistryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve lançar 409 quando ocorrer DataIntegrityViolationException ao remover registro")
    void testDeleteWaterRegistryDataIntegrityViolationConflict() {
        when(waterRegistryRepository.findById(100L)).thenReturn(Optional.of(sampleRegistry));
        doThrow(new DataIntegrityViolationException("FK constraint"))
                .when(waterRegistryRepository).delete(sampleRegistry);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                waterRegistryService.deleteWaterRegistry(100L, adminPrincipal)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Não é possível remover o registro de água"));
    }
}
