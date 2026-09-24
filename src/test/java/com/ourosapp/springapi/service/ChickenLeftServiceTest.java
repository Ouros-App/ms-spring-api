package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.chickenleft.ChickenLeftRequestDTO;
import com.ourosapp.springapi.dto.chickenleft.ChickenLeftResponseDTO;
import com.ourosapp.springapi.dto.chickenleft.ChickenLeftUpdateDTO;
import com.ourosapp.springapi.entity.ChickenLeft;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.Farm;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.repository.ChickenLeftRepository;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.repository.FarmRepository;
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
 * Testes unitários para a camada de serviço {@link ChickenLeftService}.
 */
@ExtendWith(MockitoExtension.class)
class ChickenLeftServiceTest {

    @Mock
    private ChickenLeftRepository chickenLeftRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private CompanyEmployeeRepository companyEmployeeRepository;

    @Mock
    private FarmOwnerRepository farmOwnerRepository;

    @InjectMocks
    private ChickenLeftService chickenLeftService;

    private Farm sampleFarm;
    private ChickenLeft sampleChickenLeft;
    private ChickenLeftRequestDTO sampleRequest;
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
                .chickensNow(1000)
                .place("Gleba 4")
                .idAddress(10L)
                .idEnterprise(20L)
                .build();

        sampleChickenLeft = ChickenLeft.builder()
                .id(100L)
                .chickensCount(200)
                .exitDate(LocalDate.of(2026, 9, 20))
                .idFarm(1L)
                .build();

        sampleRequest = new ChickenLeftRequestDTO(
                200,
                LocalDate.of(2026, 9, 20),
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
    @DisplayName("Deve cadastrar saída de aves com sucesso como ADM e abater saldo da fazenda")
    void testCreateChickenLeftAsAdmSuccess() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(farmRepository.save(any(Farm.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chickenLeftRepository.save(any(ChickenLeft.class))).thenReturn(sampleChickenLeft);

        ChickenLeftResponseDTO response = chickenLeftService.createChickenLeft(sampleRequest, adminPrincipal);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(200, response.chickensCount());
        assertEquals(LocalDate.of(2026, 9, 20), response.exitDate());
        assertEquals(1L, response.idFarm());

        assertEquals(800, sampleFarm.getChickensNow());
        verify(farmRepository).save(sampleFarm);
        verify(chickenLeftRepository).save(any(ChickenLeft.class));
    }

    @Test
    @DisplayName("Deve lançar 400 quando saldo de aves na fazenda for insuficiente no cadastro")
    void testCreateChickenLeftInsufficientChickensBadRequest() {
        sampleFarm.setChickensNow(150);
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                chickenLeftService.createChickenLeft(sampleRequest, adminPrincipal)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("maior que o saldo de aves atual na fazenda"));
        verify(farmRepository, never()).save(any(Farm.class));
        verify(chickenLeftRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar 400 quando ADM não informar idFarm")
    void testCreateChickenLeftAsAdmNullFarmId() {
        ChickenLeftRequestDTO requestWithoutFarm = new ChickenLeftRequestDTO(
                200,
                LocalDate.of(2026, 9, 20),
                null
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                chickenLeftService.createChickenLeft(requestWithoutFarm, adminPrincipal)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("O ID da fazenda é obrigatório"));
        verify(chickenLeftRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve cadastrar saída de aves com sucesso como COMPANY_EMPLOYEE da mesma empresa")
    void testCreateChickenLeftAsCompanyEmployeeSuccess() {
        CompanyEmployee employee = CompanyEmployee.builder().id(10L).idEnterprise(20L).build();
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(farmRepository.save(any(Farm.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chickenLeftRepository.save(any(ChickenLeft.class))).thenReturn(sampleChickenLeft);

        ChickenLeftResponseDTO response = chickenLeftService.createChickenLeft(sampleRequest, employeePrincipal);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(800, sampleFarm.getChickensNow());
        verify(companyEmployeeRepository).findById(10L);
        verify(farmRepository).save(sampleFarm);
        verify(chickenLeftRepository).save(any(ChickenLeft.class));
    }

    @Test
    @DisplayName("Deve lançar 403 quando COMPANY_EMPLOYEE tentar cadastrar em fazenda de outra empresa")
    void testCreateChickenLeftAsCompanyEmployeeDifferentEnterpriseForbidden() {
        CompanyEmployee employee = CompanyEmployee.builder().id(10L).idEnterprise(999L).build();
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                chickenLeftService.createChickenLeft(sampleRequest, employeePrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(chickenLeftRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar 400 quando COMPANY_EMPLOYEE não informar idFarm")
    void testCreateChickenLeftAsCompanyEmployeeNullFarmId() {
        ChickenLeftRequestDTO requestWithoutFarm = new ChickenLeftRequestDTO(
                200,
                LocalDate.of(2026, 9, 20),
                null
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                chickenLeftService.createChickenLeft(requestWithoutFarm, employeePrincipal)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(chickenLeftRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve cadastrar saída de aves com sucesso como FARM_OWNER da própria fazenda")
    void testCreateChickenLeftAsFarmOwnerSuccess() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(1L).build();
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(farmRepository.save(any(Farm.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chickenLeftRepository.save(any(ChickenLeft.class))).thenReturn(sampleChickenLeft);

        ChickenLeftResponseDTO response = chickenLeftService.createChickenLeft(sampleRequest, farmOwnerPrincipal);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(800, sampleFarm.getChickensNow());
        verify(farmOwnerRepository).findById(20L);
        verify(farmRepository).save(sampleFarm);
        verify(chickenLeftRepository).save(any(ChickenLeft.class));
    }

    @Test
    @DisplayName("Deve cadastrar saída de aves como FARM_OWNER inferindo idFarm quando omitido no payload")
    void testCreateChickenLeftAsFarmOwnerWithoutFarmIdSuccess() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(1L).build();
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(farmRepository.save(any(Farm.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chickenLeftRepository.save(any(ChickenLeft.class))).thenReturn(sampleChickenLeft);

        ChickenLeftRequestDTO requestWithoutFarm = new ChickenLeftRequestDTO(
                200,
                LocalDate.of(2026, 9, 20),
                null
        );

        ChickenLeftResponseDTO response = chickenLeftService.createChickenLeft(requestWithoutFarm, farmOwnerPrincipal);

        assertNotNull(response);
        assertEquals(100L, response.id());
        verify(chickenLeftRepository).save(argThat(entity -> Long.valueOf(1L).equals(entity.getIdFarm())));
    }

    @Test
    @DisplayName("Deve lançar 403 quando FARM_OWNER tentar cadastrar saída em outra fazenda")
    void testCreateChickenLeftAsFarmOwnerDifferentFarmForbidden() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(999L).build();
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                chickenLeftService.createChickenLeft(sampleRequest, farmOwnerPrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(chickenLeftRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar 400 quando FARM_OWNER não tiver fazenda vinculada")
    void testCreateChickenLeftAsFarmOwnerNullLinkedFarm() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(null).build();
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                chickenLeftService.createChickenLeft(sampleRequest, farmOwnerPrincipal)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(chickenLeftRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar 401 ao cadastrar saída de aves com principal nulo")
    void testCreateChickenLeftNullPrincipal() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                chickenLeftService.createChickenLeft(sampleRequest, null)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 403 para role não autorizada ao cadastrar saída de aves")
    void testCreateChickenLeftForbiddenRole() {
        UserPrincipal guest = new UserPrincipal(1L, "guest@test.com", "pass", "GUEST", List.of(new SimpleGrantedAuthority("ROLE_GUEST")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                chickenLeftService.createChickenLeft(sampleRequest, guest)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 409 quando ocorrer DataIntegrityViolationException ao salvar saída de aves")
    void testCreateChickenLeftDataIntegrityViolationConflict() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(farmRepository.save(any(Farm.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chickenLeftRepository.save(any(ChickenLeft.class)))
                .thenThrow(new DataIntegrityViolationException("Erro de FK"));

        assertThrows(DataIntegrityViolationException.class, () ->
                chickenLeftService.createChickenLeft(sampleRequest, adminPrincipal)
        );
    }

    // =========================================================================
    // GET ALL / FILTER TESTS
    // =========================================================================

    @Test
    @DisplayName("Deve listar todos os registros de saída como ADM sem filtro")
    void testGetChickenLeftsForAdmAll() {
        when(chickenLeftRepository.findAll()).thenReturn(List.of(sampleChickenLeft));

        List<ChickenLeftResponseDTO> result = chickenLeftService.getChickenLeftsForUser(null, adminPrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(chickenLeftRepository).findAll();
    }

    @Test
    @DisplayName("Deve listar registros de saída filtrados por fazenda como ADM")
    void testGetChickenLeftsForAdmFiltered() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(chickenLeftRepository.findAllByIdFarm(1L)).thenReturn(List.of(sampleChickenLeft));

        List<ChickenLeftResponseDTO> result = chickenLeftService.getChickenLeftsForUser(1L, adminPrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(farmRepository).findById(1L);
        verify(chickenLeftRepository).findAllByIdFarm(1L);
    }

    @Test
    @DisplayName("Deve listar registros de saída de todas as fazendas da empresa para COMPANY_EMPLOYEE")
    void testGetChickenLeftsForCompanyEmployeeAllFarms() {
        CompanyEmployee employee = CompanyEmployee.builder().id(10L).idEnterprise(20L).build();
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(farmRepository.findAllByIdEnterprise(20L)).thenReturn(List.of(sampleFarm));
        when(chickenLeftRepository.findAllByIdFarmIn(List.of(1L))).thenReturn(List.of(sampleChickenLeft));

        List<ChickenLeftResponseDTO> result = chickenLeftService.getChickenLeftsForUser(null, employeePrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(chickenLeftRepository).findAllByIdFarmIn(List.of(1L));
    }

    @Test
    @DisplayName("Deve retornar lista vazia para COMPANY_EMPLOYEE quando empresa não possuir fazendas")
    void testGetChickenLeftsForCompanyEmployeeNoFarms() {
        CompanyEmployee employee = CompanyEmployee.builder().id(10L).idEnterprise(20L).build();
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(farmRepository.findAllByIdEnterprise(20L)).thenReturn(List.of());

        List<ChickenLeftResponseDTO> result = chickenLeftService.getChickenLeftsForUser(null, employeePrincipal);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(chickenLeftRepository, never()).findAllByIdFarmIn(any());
    }

    @Test
    @DisplayName("Deve listar registros de fazenda específica para COMPANY_EMPLOYEE da mesma empresa")
    void testGetChickenLeftsForCompanyEmployeeSpecificFarmSuccess() {
        CompanyEmployee employee = CompanyEmployee.builder().id(10L).idEnterprise(20L).build();
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(chickenLeftRepository.findAllByIdFarm(1L)).thenReturn(List.of(sampleChickenLeft));

        List<ChickenLeftResponseDTO> result = chickenLeftService.getChickenLeftsForUser(1L, employeePrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(chickenLeftRepository).findAllByIdFarm(1L);
    }

    @Test
    @DisplayName("Deve lançar 403 para COMPANY_EMPLOYEE ao filtrar registros de fazenda de outra empresa")
    void testGetChickenLeftsForCompanyEmployeeDifferentEnterpriseForbidden() {
        CompanyEmployee employee = CompanyEmployee.builder().id(10L).idEnterprise(999L).build();
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                chickenLeftService.getChickenLeftsForUser(1L, employeePrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve listar registros de saída da própria fazenda para FARM_OWNER")
    void testGetChickenLeftsForFarmOwnerSuccess() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(1L).build();
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));
        when(chickenLeftRepository.findAllByIdFarm(1L)).thenReturn(List.of(sampleChickenLeft));

        List<ChickenLeftResponseDTO> result = chickenLeftService.getChickenLeftsForUser(null, farmOwnerPrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(chickenLeftRepository).findAllByIdFarm(1L);
    }

    @Test
    @DisplayName("Deve lançar 403 para FARM_OWNER ao tentar filtrar registros de outra fazenda")
    void testGetChickenLeftsForFarmOwnerDifferentFarmForbidden() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(1L).build();
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                chickenLeftService.getChickenLeftsForUser(999L, farmOwnerPrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve retornar lista vazia para FARM_OWNER quando não possuir fazenda vinculada")
    void testGetChickenLeftsForFarmOwnerNullLinkedFarm() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(null).build();
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));

        List<ChickenLeftResponseDTO> result = chickenLeftService.getChickenLeftsForUser(null, farmOwnerPrincipal);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Deve lançar 403 para role inválida ao listar registros de saída")
    void testGetChickenLeftsForInvalidRoleForbidden() {
        UserPrincipal guest = new UserPrincipal(1L, "guest@test.com", "pass", "GUEST", List.of(new SimpleGrantedAuthority("ROLE_GUEST")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                chickenLeftService.getChickenLeftsForUser(null, guest)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // =========================================================================
    // GET BY ID TESTS
    // =========================================================================

    @Test
    @DisplayName("Deve buscar registro de saída por ID com sucesso como ADM")
    void testGetChickenLeftByIdAsAdmSuccess() {
        when(chickenLeftRepository.findById(100L)).thenReturn(Optional.of(sampleChickenLeft));

        ChickenLeftResponseDTO response = chickenLeftService.getChickenLeftById(100L, adminPrincipal);

        assertNotNull(response);
        assertEquals(100L, response.id());
    }

    @Test
    @DisplayName("Deve buscar registro de saída por ID como COMPANY_EMPLOYEE da mesma empresa")
    void testGetChickenLeftByIdAsCompanyEmployeeSuccess() {
        CompanyEmployee employee = CompanyEmployee.builder().id(10L).idEnterprise(20L).build();
        when(chickenLeftRepository.findById(100L)).thenReturn(Optional.of(sampleChickenLeft));
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));

        ChickenLeftResponseDTO response = chickenLeftService.getChickenLeftById(100L, employeePrincipal);

        assertNotNull(response);
        assertEquals(100L, response.id());
    }

    @Test
    @DisplayName("Deve lançar 403 ao buscar registro de saída por ID como COMPANY_EMPLOYEE de outra empresa")
    void testGetChickenLeftByIdAsCompanyEmployeeDifferentEnterpriseForbidden() {
        CompanyEmployee employee = CompanyEmployee.builder().id(10L).idEnterprise(999L).build();
        when(chickenLeftRepository.findById(100L)).thenReturn(Optional.of(sampleChickenLeft));
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                chickenLeftService.getChickenLeftById(100L, employeePrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve buscar registro de saída por ID como FARM_OWNER da mesma fazenda")
    void testGetChickenLeftByIdAsFarmOwnerSuccess() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(1L).build();
        when(chickenLeftRepository.findById(100L)).thenReturn(Optional.of(sampleChickenLeft));
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));

        ChickenLeftResponseDTO response = chickenLeftService.getChickenLeftById(100L, farmOwnerPrincipal);

        assertNotNull(response);
        assertEquals(100L, response.id());
    }

    @Test
    @DisplayName("Deve lançar 403 ao buscar registro de saída por ID como FARM_OWNER de outra fazenda")
    void testGetChickenLeftByIdAsFarmOwnerDifferentFarmForbidden() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(999L).build();
        when(chickenLeftRepository.findById(100L)).thenReturn(Optional.of(sampleChickenLeft));
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                chickenLeftService.getChickenLeftById(100L, farmOwnerPrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 404 ao buscar registro inexistente por ID")
    void testGetChickenLeftByIdNotFound() {
        when(chickenLeftRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                chickenLeftService.getChickenLeftById(999L, adminPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 404 ao buscar registro com ID nulo")
    void testGetChickenLeftByIdNull() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                chickenLeftService.getChickenLeftById(null, adminPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // =========================================================================
    // UPDATE TESTS
    // =========================================================================

    @Test
    @DisplayName("Deve atualizar parcialmente registro de saída com delta positivo e abater saldo da fazenda")
    void testUpdateChickenLeftPositiveDeltaSuccess() {
        when(chickenLeftRepository.findById(100L)).thenReturn(Optional.of(sampleChickenLeft));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(farmRepository.save(any(Farm.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chickenLeftRepository.save(any(ChickenLeft.class))).thenAnswer(inv -> inv.getArgument(0));

        ChickenLeftUpdateDTO updateDTO = new ChickenLeftUpdateDTO(300, LocalDate.of(2026, 9, 21));

        ChickenLeftResponseDTO response = chickenLeftService.updateChickenLeft(100L, updateDTO, adminPrincipal);

        assertNotNull(response);
        assertEquals(300, response.chickensCount());
        assertEquals(LocalDate.of(2026, 9, 21), response.exitDate());
        assertEquals(900, sampleFarm.getChickensNow());
        verify(farmRepository).save(sampleFarm);
        verify(chickenLeftRepository).save(sampleChickenLeft);
    }

    @Test
    @DisplayName("Deve atualizar parcialmente registro de saída com delta negativo e devolver saldo à fazenda")
    void testUpdateChickenLeftNegativeDeltaSuccess() {
        when(chickenLeftRepository.findById(100L)).thenReturn(Optional.of(sampleChickenLeft));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(farmRepository.save(any(Farm.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chickenLeftRepository.save(any(ChickenLeft.class))).thenAnswer(inv -> inv.getArgument(0));

        ChickenLeftUpdateDTO updateDTO = new ChickenLeftUpdateDTO(150, null);

        ChickenLeftResponseDTO response = chickenLeftService.updateChickenLeft(100L, updateDTO, adminPrincipal);

        assertNotNull(response);
        assertEquals(150, response.chickensCount());
        assertEquals(1050, sampleFarm.getChickensNow());
        verify(farmRepository).save(sampleFarm);
    }

    @Test
    @DisplayName("Deve lançar 400 ao atualizar saída quando delta positivo for maior que saldo da fazenda")
    void testUpdateChickenLeftInsufficientChickensBadRequest() {
        sampleFarm.setChickensNow(50);
        when(chickenLeftRepository.findById(100L)).thenReturn(Optional.of(sampleChickenLeft));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));

        ChickenLeftUpdateDTO updateDTO = new ChickenLeftUpdateDTO(300, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                chickenLeftService.updateChickenLeft(100L, updateDTO, adminPrincipal)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("A quantidade adicional de aves de saída"));
        verify(farmRepository, never()).save(any(Farm.class));
    }

    @Test
    @DisplayName("Deve retornar registro inalterado quando DTO não tiver atualizações (hasUpdates = false)")
    void testUpdateChickenLeftNoUpdates() {
        when(chickenLeftRepository.findById(100L)).thenReturn(Optional.of(sampleChickenLeft));

        ChickenLeftUpdateDTO emptyUpdate = new ChickenLeftUpdateDTO(null, null);

        ChickenLeftResponseDTO response = chickenLeftService.updateChickenLeft(100L, emptyUpdate, adminPrincipal);

        assertNotNull(response);
        assertEquals(100L, response.id());
        verify(chickenLeftRepository, never()).save(any());
        verify(farmRepository, never()).save(any(Farm.class));
    }

    @Test
    @DisplayName("Deve atualizar apenas exitDate sem alterar quantidade ou saldo da fazenda")
    void testUpdateChickenLeftOnlyDateSuccess() {
        when(chickenLeftRepository.findById(100L)).thenReturn(Optional.of(sampleChickenLeft));
        when(chickenLeftRepository.save(any(ChickenLeft.class))).thenAnswer(inv -> inv.getArgument(0));

        ChickenLeftUpdateDTO updateDTO = new ChickenLeftUpdateDTO(null, LocalDate.of(2026, 9, 22));

        ChickenLeftResponseDTO response = chickenLeftService.updateChickenLeft(100L, updateDTO, adminPrincipal);

        assertNotNull(response);
        assertEquals(LocalDate.of(2026, 9, 22), response.exitDate());
        assertEquals(200, response.chickensCount());
        verify(farmRepository, never()).save(any(Farm.class));
        verify(chickenLeftRepository).save(sampleChickenLeft);
    }

    @Test
    @DisplayName("Deve lançar 403 ao atualizar registro como COMPANY_EMPLOYEE de outra empresa")
    void testUpdateChickenLeftAsCompanyEmployeeDifferentEnterpriseForbidden() {
        CompanyEmployee employee = CompanyEmployee.builder().id(10L).idEnterprise(999L).build();
        when(chickenLeftRepository.findById(100L)).thenReturn(Optional.of(sampleChickenLeft));
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));

        ChickenLeftUpdateDTO updateDTO = new ChickenLeftUpdateDTO(250, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                chickenLeftService.updateChickenLeft(100L, updateDTO, employeePrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(chickenLeftRepository, never()).save(any());
    }

    // =========================================================================
    // DELETE TESTS
    // =========================================================================

    @Test
    @DisplayName("Deve remover registro de saída com sucesso como ADM e estornar saldo de aves na fazenda")
    void testDeleteChickenLeftAsAdmSuccess() {
        when(chickenLeftRepository.findById(100L)).thenReturn(Optional.of(sampleChickenLeft));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(farmRepository.save(any(Farm.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(chickenLeftRepository).delete(sampleChickenLeft);

        assertDoesNotThrow(() -> chickenLeftService.deleteChickenLeft(100L, adminPrincipal));

        assertEquals(1200, sampleFarm.getChickensNow());
        verify(farmRepository).save(sampleFarm);
        verify(chickenLeftRepository).delete(sampleChickenLeft);
        verify(chickenLeftRepository).flush();
    }

    @Test
    @DisplayName("Deve remover registro de saída com sucesso como FARM_OWNER da própria fazenda")
    void testDeleteChickenLeftAsFarmOwnerSuccess() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(1L).build();
        when(chickenLeftRepository.findById(100L)).thenReturn(Optional.of(sampleChickenLeft));
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(farmRepository.save(any(Farm.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(chickenLeftRepository).delete(sampleChickenLeft);

        assertDoesNotThrow(() -> chickenLeftService.deleteChickenLeft(100L, farmOwnerPrincipal));

        assertEquals(1200, sampleFarm.getChickensNow());
        verify(chickenLeftRepository).delete(sampleChickenLeft);
    }

    @Test
    @DisplayName("Deve lançar 403 ao remover registro como FARM_OWNER de outra fazenda")
    void testDeleteChickenLeftAsFarmOwnerDifferentFarmForbidden() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(999L).build();
        when(chickenLeftRepository.findById(100L)).thenReturn(Optional.of(sampleChickenLeft));
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                chickenLeftService.deleteChickenLeft(100L, farmOwnerPrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(chickenLeftRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve lançar 404 ao remover registro inexistente")
    void testDeleteChickenLeftNotFound() {
        when(chickenLeftRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                chickenLeftService.deleteChickenLeft(999L, adminPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(chickenLeftRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve lançar 409 quando ocorrer DataIntegrityViolationException ao remover registro")
    void testDeleteChickenLeftDataIntegrityViolationConflict() {
        when(chickenLeftRepository.findById(100L)).thenReturn(Optional.of(sampleChickenLeft));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(farmRepository.save(any(Farm.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new DataIntegrityViolationException("FK constraint"))
                .when(chickenLeftRepository).delete(sampleChickenLeft);

        assertThrows(DataIntegrityViolationException.class, () ->
                chickenLeftService.deleteChickenLeft(100L, adminPrincipal)
        );
    }

    @Test
    @DisplayName("Deve lançar 401 quando principal tiver ID nulo")
    void testEnsureAuthenticatedNullId() {
        UserPrincipal principalWithNullId = new UserPrincipal(null, "user@test.com", "pass", "ADM", List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                chickenLeftService.getChickenLeftsForUser(null, principalWithNullId)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 404 quando funcionário não for encontrado no repositório")
    void testGetCompanyEmployeeNotFound() {
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                chickenLeftService.getChickenLeftsForUser(null, employeePrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Funcionário logado não encontrado"));
    }

    @Test
    @DisplayName("Deve lançar 404 quando produtor rural não for encontrado no repositório")
    void testGetFarmOwnerNotFound() {
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                chickenLeftService.getChickenLeftsForUser(null, farmOwnerPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Produtor rural logado não encontrado"));
    }

    @Test
    @DisplayName("Deve lançar 404 quando fazenda não for encontrada por ID")
    void testFindFarmByIdNotFound() {
        when(farmRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                chickenLeftService.getChickenLeftsForUser(999L, adminPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Fazenda não encontrada"));
    }

    @Test
    @DisplayName("Deve lançar 403 em mutação quando role do usuário for desconhecida")
    void testValidateChickenLeftMutationUnknownRole() {
        UserPrincipal unknownUser = new UserPrincipal(99L, "unknown@test.com", "pass", "UNKNOWN_ROLE", List.of());
        when(chickenLeftRepository.findById(100L)).thenReturn(Optional.of(sampleChickenLeft));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                chickenLeftService.deleteChickenLeft(100L, unknownUser)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Perfil de usuário sem permissão para remover"));
    }

    @Test
    @DisplayName("Deve tratar chickensNow nulo como zero na criação")
    void testCreateChickenLeftWithNullChickensNow() {
        sampleFarm.setChickensNow(null);
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                chickenLeftService.createChickenLeft(sampleRequest, adminPrincipal)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve remover e estornar saldo quando chickensNow for nulo na fazenda")
    void testDeleteChickenLeftWithNullChickensNowOnFarm() {
        sampleFarm.setChickensNow(null);
        when(chickenLeftRepository.findById(100L)).thenReturn(Optional.of(sampleChickenLeft));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(farmRepository.save(any(Farm.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> chickenLeftService.deleteChickenLeft(100L, adminPrincipal));

        assertEquals(200, sampleFarm.getChickensNow());
    }
}
