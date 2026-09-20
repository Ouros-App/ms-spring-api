package com.ourosapp.springapi.service;

import static com.ourosapp.springapi.constants.RoleConstants.ADM;
import static com.ourosapp.springapi.constants.RoleConstants.COMPANY_EMPLOYEE;
import static com.ourosapp.springapi.constants.RoleConstants.FARM_OWNER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.ourosapp.springapi.dto.farm.FarmResponseDTO;
import com.ourosapp.springapi.dto.stategoal.RegionGoalRequestDTO;
import com.ourosapp.springapi.dto.stategoal.StateGoalRequestDTO;
import com.ourosapp.springapi.dto.stategoal.StateGoalResponseDTO;
import com.ourosapp.springapi.dto.stategoal.StateGoalUpdateDTO;
import com.ourosapp.springapi.entity.*;
import com.ourosapp.springapi.repository.*;
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

/**
 * Testes unitários para a camada de serviço {@link StateGoalService}.
 */
@ExtendWith(MockitoExtension.class)
class StateGoalServiceTest {

    @Mock
    private StateGoalRepository stateGoalRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private CompanyEmployeeRepository companyEmployeeRepository;

    @Mock
    private FarmOwnerRepository farmOwnerRepository;

    @Mock
    private FarmGoalRepository farmGoalRepository;

    @Mock
    private RegionGoalRepository regionGoalRepository;

    @Mock
    private StateGoalRegionRepository stateGoalRegionRepository;

    @InjectMocks
    private StateGoalService stateGoalService;

    private UserPrincipal admPrincipal;
    private UserPrincipal employeePrincipal;
    private UserPrincipal ownerPrincipal;
    private Farm farm;
    private Farm farm2;
    private StateGoal goal;
    private StateGoal goal2;
    private StateGoalRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        admPrincipal = new UserPrincipal(1L, "adm@ouros.com", null, ADM, List.of(new SimpleGrantedAuthority("ROLE_ADM")));
        employeePrincipal = new UserPrincipal(2L, "employee@ouros.com", null, COMPANY_EMPLOYEE, List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE")));
        ownerPrincipal = new UserPrincipal(3L, "owner@ouros.com", null, FARM_OWNER, List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER")));

        farm = Farm.builder()
                .id(10L)
                .name("Fazenda Teste")
                .areaProperty(new BigDecimal("100.00"))
                .region("Sudeste")
                .poultryCapacity(20000)
                .place("Gleba 1")
                .idAddress(100L)
                .idEnterprise(50L)
                .build();

        farm2 = Farm.builder()
                .id(20L)
                .name("Fazenda Secundária")
                .areaProperty(new BigDecimal("50.00"))
                .region("Sudeste")
                .poultryCapacity(15000)
                .place("Gleba 2")
                .idAddress(101L)
                .idEnterprise(50L)
                .build();

        goal = StateGoal.builder()
                .id(1L)
                .title("Meta Regional SP")
                .description("Meta estadual")
                .type("FEED_CONVERSION")
                .status("IN_PROGRESS")
                .targetValue(new BigDecimal("1.6500"))
                .dateCreation(LocalDate.of(2026, 1, 1))
                .dateEnd(LocalDate.of(2026, 12, 31))
                .idFarm(10L)
                .build();

        goal2 = StateGoal.builder()
                .id(2L)
                .title("Meta Regional Secundária")
                .description("Meta estadual vinculada")
                .type("FEED_CONVERSION")
                .status("IN_PROGRESS")
                .targetValue(new BigDecimal("1.7000"))
                .dateCreation(LocalDate.of(2026, 1, 1))
                .dateEnd(LocalDate.of(2026, 12, 31))
                .idFarm(20L)
                .build();

        requestDTO = new StateGoalRequestDTO(
                "Meta Regional SP",
                "Meta estadual",
                "FEED_CONVERSION",
                "IN_PROGRESS",
                new BigDecimal("1.6500"),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                10L,
                "Sudeste"
        );
    }

    // ==========================================
    // CREATE STATE GOAL
    // ==========================================

    @Test
    @DisplayName("Deve criar meta estadual com sucesso para ADM")
    void deveCriarMetaEstadualComSucessoParaAdm() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(stateGoalRepository.save(any(StateGoal.class))).thenReturn(goal);
        when(regionGoalRepository.save(any(RegionGoal.class)))
                .thenReturn(RegionGoal.builder().id(100L).region("Sudeste").idGoal(1L).build());

        StateGoalResponseDTO response = stateGoalService.createStateGoal(requestDTO, admPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Meta Regional SP", response.title());
        assertEquals("Sudeste", response.region());
        verify(stateGoalRepository, times(1)).save(any(StateGoal.class));
        verify(farmGoalRepository, times(1)).save(any(FarmGoal.class));
        verify(regionGoalRepository, times(1)).save(any(RegionGoal.class));
        verify(stateGoalRegionRepository, times(1)).save(any(StateGoalRegion.class));
    }

    @Test
    @DisplayName("Deve criar meta estadual com sucesso para COMPANY_EMPLOYEE da mesma empresa")
    void deveCriarMetaEstadualComSucessoParaCompanyEmployee() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(50L).build();
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(stateGoalRepository.save(any(StateGoal.class))).thenReturn(goal);
        when(regionGoalRepository.save(any(RegionGoal.class)))
                .thenReturn(RegionGoal.builder().id(100L).region("Sudeste").idGoal(1L).build());

        StateGoalResponseDTO response = stateGoalService.createStateGoal(requestDTO, employeePrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    @Test
    @DisplayName("Deve criar meta estadual para FARM_OWNER inferindo idFarm automaticamente")
    void deveCriarMetaEstadualComSucessoParaFarmOwner() {
        StateGoalRequestDTO dtoWithoutFarm = new StateGoalRequestDTO(
                "Meta Regional SP", "Meta estadual", "FEED_CONVERSION", "IN_PROGRESS",
                new BigDecimal("1.6500"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                null, null
        );
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(stateGoalRepository.save(any(StateGoal.class))).thenReturn(goal);
        when(regionGoalRepository.save(any(RegionGoal.class)))
                .thenReturn(RegionGoal.builder().id(100L).region("Sudeste").idGoal(1L).build());

        StateGoalResponseDTO response = stateGoalService.createStateGoal(dtoWithoutFarm, ownerPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    @Test
    @DisplayName("Deve lançar 401 Unauthorized quando principal for nulo")
    void deveLancar401QuandoPrincipalNulo() {
        assertThrows(ResponseStatusException.class, () -> stateGoalService.createStateGoal(requestDTO, null));
    }

    @Test
    @DisplayName("Deve lançar 400 Bad Request se a data de término for anterior à data de criação")
    void deveLancar400QuandoDataTerminoAnteriorACriacao() {
        StateGoalRequestDTO invalidDates = new StateGoalRequestDTO(
                "Meta Regional SP", "Meta estadual", "FEED_CONVERSION", "IN_PROGRESS",
                new BigDecimal("1.6500"), LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1),
                10L, "Sudeste"
        );
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> stateGoalService.createStateGoal(invalidDates, admPrincipal));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 403 Forbidden quando COMPANY_EMPLOYEE tentar acessar fazenda de outra empresa")
    void deveLancar403ParaCompanyEmployeeDeOutraEmpresa() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(999L).build();
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> stateGoalService.createStateGoal(requestDTO, employeePrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 403 Forbidden quando FARM_OWNER tentar acessar outra fazenda")
    void deveLancar403ParaFarmOwnerDeOutraFazenda() {
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(999L).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> stateGoalService.createStateGoal(requestDTO, ownerPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 400 Bad Request quando FARM_OWNER não tiver fazenda vinculada")
    void deveLancar400QuandoFarmOwnerSemFazenda() {
        StateGoalRequestDTO dtoWithoutFarm = new StateGoalRequestDTO(
                "Meta", "Desc", "FEED_CONVERSION", "IN_PROGRESS",
                new BigDecimal("1.6500"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                null, null
        );
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(null).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> stateGoalService.createStateGoal(dtoWithoutFarm, ownerPrincipal));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 400 Bad Request quando ADM não informar idFarm")
    void deveLancar400QuandoAdmNaoInformarFarmId() {
        StateGoalRequestDTO dtoWithoutFarm = new StateGoalRequestDTO(
                "Meta", "Desc", "FEED_CONVERSION", "IN_PROGRESS",
                new BigDecimal("1.6500"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                null, null
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> stateGoalService.createStateGoal(dtoWithoutFarm, admPrincipal));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 409 Conflict quando ocorrer DataIntegrityViolationException no cadastro")
    void deveLancar409QuandoDataIntegrityViolationNoCadastro() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(stateGoalRepository.save(any(StateGoal.class))).thenThrow(new DataIntegrityViolationException("Erro de FK"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> stateGoalService.createStateGoal(requestDTO, admPrincipal));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ==========================================
    // GET STATE GOALS FOR USER
    // ==========================================

    @Test
    @DisplayName("Deve listar metas com filtro de fazenda e região compatível")
    void deveListarMetasComFiltroFazendaERegiao() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(farmGoalRepository.findByIdFarm(10L)).thenReturn(List.of());
        when(stateGoalRepository.findByIdFarm(10L)).thenReturn(List.of(goal));

        List<StateGoalResponseDTO> result = stateGoalService.getStateGoalsForUser(10L, "Sudeste", admPrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Deve filtrar fazenda única por qualquer região e manter a região primária na resposta")
    void deveFiltrarFazendaUnicaPorQualquerRegiao() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(farmGoalRepository.findByIdFarm(10L)).thenReturn(List.of());
        when(stateGoalRepository.findByIdFarm(10L)).thenReturn(List.of(goal));
        when(regionGoalRepository.findByIdGoal(1L)).thenReturn(List.of(
                RegionGoal.builder().id(10L).region("Sudeste").idGoal(1L).build(),
                RegionGoal.builder().id(11L).region("Sul").idGoal(1L).build()
        ));

        List<StateGoalResponseDTO> result = stateGoalService.getStateGoalsForUser(10L, "Sul", admPrincipal);

        assertEquals(1, result.size());
        assertEquals("Sudeste", result.get(0).region());
    }

    @Test
    @DisplayName("Deve listar metas com filtro de fazenda unindo metas diretas e vinculadas via farm_goals")
    void deveListarMetasComFiltroFazendaIncluindoJuncaoFarmGoals() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(farmGoalRepository.findByIdFarm(10L))
                .thenReturn(List.of(FarmGoal.builder().id(1L).idFarm(10L).idGoal(2L).build()));
        when(stateGoalRepository.findByIdFarm(10L)).thenReturn(List.of(goal));
        when(stateGoalRepository.findAllById(List.of(2L))).thenReturn(List.of(goal2));

        List<StateGoalResponseDTO> result = stateGoalService.getStateGoalsForUser(10L, null, admPrincipal);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando filtro de região não coincidir com a fazenda")
    void deveRetornarVazioQuandoFiltroRegiaoIncompativel() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));

        List<StateGoalResponseDTO> result = stateGoalService.getStateGoalsForUser(10L, "Nordeste", admPrincipal);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Deve listar metas para ADM sem filtros")
    void deveListarMetasParaAdmSemFiltros() {
        when(stateGoalRepository.findAll()).thenReturn(List.of(goal));
        when(farmRepository.findAllById(List.of(10L))).thenReturn(List.of(farm));

        List<StateGoalResponseDTO> result = stateGoalService.getStateGoalsForUser(null, null, admPrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Deve listar metas para ADM com filtro de região")
    void deveListarMetasParaAdmComFiltroRegiao() {
        when(stateGoalRepository.findAll()).thenReturn(List.of(goal));
        when(farmRepository.findAllById(List.of(10L))).thenReturn(List.of(farm));

        List<StateGoalResponseDTO> result = stateGoalService.getStateGoalsForUser(null, "Sudeste", admPrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Deve filtrar metas do ADM por qualquer região e manter a região primária na resposta")
    void deveFiltrarMetasDoAdmPorQualquerRegiao() {
        when(stateGoalRepository.findAll()).thenReturn(List.of(goal));
        when(farmRepository.findAllById(List.of(10L))).thenReturn(List.of(farm));
        when(regionGoalRepository.findByIdGoal(1L)).thenReturn(List.of(
                RegionGoal.builder().id(10L).region("Sudeste").idGoal(1L).build(),
                RegionGoal.builder().id(11L).region("Sul").idGoal(1L).build()
        ));

        List<StateGoalResponseDTO> result = stateGoalService.getStateGoalsForUser(null, "Sul", admPrincipal);

        assertEquals(1, result.size());
        assertEquals("Sudeste", result.get(0).region());
    }

    @Test
    @DisplayName("Deve retornar vazio para ADM quando não houver metas cadastradas")
    void deveRetornarVazioParaAdmSemMetas() {
        when(stateGoalRepository.findAll()).thenReturn(List.of());

        List<StateGoalResponseDTO> result = stateGoalService.getStateGoalsForUser(null, null, admPrincipal);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Deve listar metas para COMPANY_EMPLOYEE de sua empresa")
    void deveListarMetasParaCompanyEmployee() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(50L).build();
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(farmRepository.findAllByIdEnterprise(50L)).thenReturn(List.of(farm));
        when(stateGoalRepository.findByIdFarmIn(List.of(10L))).thenReturn(List.of(goal));
        when(farmGoalRepository.findByIdFarmIn(List.of(10L))).thenReturn(List.of());

        List<StateGoalResponseDTO> result = stateGoalService.getStateGoalsForUser(null, "Sudeste", employeePrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Deve filtrar metas do COMPANY_EMPLOYEE por qualquer região e manter a região primária na resposta")
    void deveFiltrarMetasDoCompanyEmployeePorQualquerRegiao() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(50L).build();
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(farmRepository.findAllByIdEnterprise(50L)).thenReturn(List.of(farm));
        when(stateGoalRepository.findByIdFarmIn(List.of(10L))).thenReturn(List.of(goal));
        when(farmGoalRepository.findByIdFarmIn(List.of(10L))).thenReturn(List.of());
        when(regionGoalRepository.findByIdGoal(1L)).thenReturn(List.of(
                RegionGoal.builder().id(10L).region("Sudeste").idGoal(1L).build(),
                RegionGoal.builder().id(11L).region("Sul").idGoal(1L).build()
        ));

        List<StateGoalResponseDTO> result = stateGoalService.getStateGoalsForUser(null, "Sul", employeePrincipal);

        assertEquals(1, result.size());
        assertEquals("Sudeste", result.get(0).region());
    }

    @Test
    @DisplayName("Deve listar metas para COMPANY_EMPLOYEE incluindo metas vinculadas via farm_goals")
    void deveListarMetasParaCompanyEmployeeIncluindoJuncaoFarmGoals() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(50L).build();
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(farmRepository.findAllByIdEnterprise(50L)).thenReturn(List.of(farm));
        when(stateGoalRepository.findByIdFarmIn(List.of(10L))).thenReturn(List.of(goal));
        when(farmGoalRepository.findByIdFarmIn(List.of(10L)))
                .thenReturn(List.of(FarmGoal.builder().id(1L).idFarm(10L).idGoal(2L).build()));
        when(stateGoalRepository.findAllById(List.of(2L))).thenReturn(List.of(goal2));

        List<StateGoalResponseDTO> result = stateGoalService.getStateGoalsForUser(null, null, employeePrincipal);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Deve retornar vazio para COMPANY_EMPLOYEE quando empresa não tiver fazendas")
    void deveRetornarVazioParaCompanyEmployeeSemFazendas() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(50L).build();
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(farmRepository.findAllByIdEnterprise(50L)).thenReturn(List.of());

        List<StateGoalResponseDTO> result = stateGoalService.getStateGoalsForUser(null, null, employeePrincipal);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Deve listar metas para FARM_OWNER de sua fazenda")
    void deveListarMetasParaFarmOwner() {
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(farmGoalRepository.findByIdFarm(10L)).thenReturn(List.of());
        when(stateGoalRepository.findByIdFarm(10L)).thenReturn(List.of(goal));

        List<StateGoalResponseDTO> result = stateGoalService.getStateGoalsForUser(null, "Sudeste", ownerPrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Deve listar metas para FARM_OWNER unindo metas diretas e junção farm_goals")
    void deveListarMetasParaFarmOwnerIncluindoJuncaoFarmGoals() {
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(farmGoalRepository.findByIdFarm(10L))
                .thenReturn(List.of(FarmGoal.builder().id(1L).idFarm(10L).idGoal(2L).build()));
        when(stateGoalRepository.findByIdFarm(10L)).thenReturn(List.of(goal));
        when(stateGoalRepository.findAllById(List.of(2L))).thenReturn(List.of(goal2));

        List<StateGoalResponseDTO> result = stateGoalService.getStateGoalsForUser(null, null, ownerPrincipal);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Deve retornar vazio para FARM_OWNER sem fazenda vinculada")
    void deveRetornarVazioParaFarmOwnerSemFazenda() {
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(null).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));

        List<StateGoalResponseDTO> result = stateGoalService.getStateGoalsForUser(null, null, ownerPrincipal);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Deve lançar 403 Forbidden para perfil desconhecido na listagem")
    void deveLancar403ParaPerfilDesconhecidoNaListagem() {
        UserPrincipal invalidRolePrincipal = new UserPrincipal(9L, "user@test.com", null, "GUEST", List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> stateGoalService.getStateGoalsForUser(null, null, invalidRolePrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ==========================================
    // GET STATE GOAL BY ID
    // ==========================================

    @Test
    @DisplayName("Deve buscar meta estadual por ID com sucesso")
    void deveBuscarMetaPorIdComSucesso() {
        when(stateGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));

        StateGoalResponseDTO response = stateGoalService.getStateGoalById(1L, admPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    @Test
    @DisplayName("Deve buscar meta estadual por ID retornando a região personalizada da tabela regions_goals")
    void deveBuscarMetaPorIdComRegiaoPersonalizada() {
        when(stateGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(regionGoalRepository.findByIdGoal(1L))
                .thenReturn(List.of(RegionGoal.builder().id(50L).region("Centro-Oeste").idGoal(1L).build()));

        StateGoalResponseDTO response = stateGoalService.getStateGoalById(1L, admPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Centro-Oeste", response.region());
    }

    @Test
    @DisplayName("Deve lançar 404 quando ID for nulo ou não encontrado")
    void deveLancar404QuandoMetaNaoEncontrada() {
        when(stateGoalRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> stateGoalService.getStateGoalById(999L, admPrincipal));
        assertThrows(ResponseStatusException.class, () -> stateGoalService.getStateGoalById(null, admPrincipal));
    }

    @Test
    @DisplayName("Deve buscar meta estadual por ID com sucesso para FARM_OWNER de fazenda secundária vinculada")
    void deveBuscarMetaPorIdComSucessoParaFarmOwnerDeFazendaSecundaria() {
        FarmOwner secondaryOwner = FarmOwner.builder().id(3L).idFarm(20L).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(secondaryOwner));
        when(stateGoalRepository.findById(1L)).thenReturn(Optional.of(goal)); // goal.idFarm = 10L
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(farmGoalRepository.existsByIdFarmAndIdGoal(20L, 1L)).thenReturn(true);

        StateGoalResponseDTO response = stateGoalService.getStateGoalById(1L, ownerPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    @Test
    @DisplayName("Deve permitir leitura para COMPANY_EMPLOYEE quando fazenda secundária pertence à sua empresa")
    void devePermitirLeituraParaCompanyEmployeePorFazendaSecundaria() {
        Farm foreignPrimaryFarm = Farm.builder()
                .id(10L)
                .region("Sudeste")
                .idEnterprise(999L)
                .build();
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(50L).build();
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(stateGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(foreignPrimaryFarm));
        when(farmGoalRepository.findByIdGoal(1L)).thenReturn(List.of(
                FarmGoal.builder().id(1L).idFarm(10L).idGoal(1L).build(),
                FarmGoal.builder().id(2L).idFarm(20L).idGoal(1L).build()
        ));
        when(farmRepository.findAllById(List.of(20L))).thenReturn(List.of(farm2));

        StateGoalResponseDTO response = stateGoalService.getStateGoalById(1L, employeePrincipal);

        assertEquals(1L, response.id());
    }

    @Test
    @DisplayName("Deve lançar 403 ao buscar meta por ID para FARM_OWNER de fazenda não vinculada")
    void deveLancar403AoBuscarMetaPorIdParaFarmOwnerSemVinculo() {
        FarmOwner unlinkedOwner = FarmOwner.builder().id(3L).idFarm(99L).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(unlinkedOwner));
        when(stateGoalRepository.findById(1L)).thenReturn(Optional.of(goal)); // goal.idFarm = 10L
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(farmGoalRepository.existsByIdFarmAndIdGoal(99L, 1L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> stateGoalService.getStateGoalById(1L, ownerPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ==========================================
    // UPDATE STATE GOAL
    // ==========================================

    @Test
    @DisplayName("Deve atualizar meta estadual com sucesso")
    void deveAtualizarMetaEstadualComSucesso() {
        StateGoalUpdateDTO updateDTO = new StateGoalUpdateDTO(
                "ACHIEVED",
                LocalDate.of(2026, 11, 30),
                new BigDecimal("1.5500")
        );

        when(stateGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(stateGoalRepository.save(any(StateGoal.class))).thenReturn(goal);

        StateGoalResponseDTO response = stateGoalService.updateStateGoal(1L, updateDTO, admPrincipal);

        assertNotNull(response);
        verify(stateGoalRepository, times(1)).save(goal);
    }

    @Test
    @DisplayName("Deve atualizar meta estadual mantendo a região personalizada da tabela regions_goals")
    void deveAtualizarMetaEstadualMantendoRegiaoPersonalizada() {
        StateGoalUpdateDTO updateDTO = new StateGoalUpdateDTO(
                "ACHIEVED",
                LocalDate.of(2026, 11, 30),
                new BigDecimal("1.5500")
        );

        when(stateGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(regionGoalRepository.findByIdGoal(1L))
                .thenReturn(List.of(RegionGoal.builder().id(50L).region("Centro-Oeste").idGoal(1L).build()));
        when(stateGoalRepository.save(any(StateGoal.class))).thenReturn(goal);

        StateGoalResponseDTO response = stateGoalService.updateStateGoal(1L, updateDTO, admPrincipal);

        assertNotNull(response);
        assertEquals("Centro-Oeste", response.region());
        verify(stateGoalRepository, times(1)).save(goal);
    }

    @Test
    @DisplayName("Deve retornar sem alterações quando DTO não tiver atualizações")
    void deveRetornarSemAlteracoesQuandoSemUpdates() {
        StateGoalUpdateDTO noUpdates = new StateGoalUpdateDTO(null, null, null);
        when(stateGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));

        StateGoalResponseDTO response = stateGoalService.updateStateGoal(1L, noUpdates, admPrincipal);

        assertNotNull(response);
        verify(stateGoalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar 400 Bad Request se nova data de término for anterior à data de criação")
    void deveLancar400AoAtualizarDataTerminoInvalida() {
        StateGoalUpdateDTO invalidDate = new StateGoalUpdateDTO(null, LocalDate.of(2025, 1, 1), null);
        when(stateGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> stateGoalService.updateStateGoal(1L, invalidDate, admPrincipal));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 409 Conflict quando ocorrer DataIntegrityViolationException no update")
    void deveLancar409QuandoDataIntegrityViolationNoUpdate() {
        StateGoalUpdateDTO updateDTO = new StateGoalUpdateDTO("ACHIEVED", null, null);
        when(stateGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(stateGoalRepository.save(any(StateGoal.class))).thenThrow(new DataIntegrityViolationException("Erro"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> stateGoalService.updateStateGoal(1L, updateDTO, admPrincipal));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ==========================================
    // DELETE STATE GOAL
    // ==========================================

    @Test
    @DisplayName("Deve remover meta estadual e suas associações com sucesso")
    void deveRemoverMetaEstadualComSucesso() {
        when(stateGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));

        assertDoesNotThrow(() -> stateGoalService.deleteStateGoal(1L, admPrincipal));
        verify(stateGoalRegionRepository, times(1)).deleteByIdGoal(1L);
        verify(regionGoalRepository, times(1)).deleteByIdGoal(1L);
        verify(farmGoalRepository, times(1)).deleteByIdGoal(1L);
        verify(stateGoalRepository, times(1)).delete(goal);
    }

    // ==========================================
    // FARM ASSOCIATIONS (farm_goals)
    // ==========================================

    @Test
    @DisplayName("Deve vincular fazenda a meta estadual (farm_goals)")
    void deveVincularFazendaAMetaEstadual() {
        when(stateGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(farmRepository.findById(20L)).thenReturn(Optional.of(farm2));
        when(farmGoalRepository.existsByIdFarmAndIdGoal(20L, 1L)).thenReturn(false);

        assertDoesNotThrow(() -> stateGoalService.addFarmToStateGoal(1L, 20L, admPrincipal));
        verify(farmGoalRepository, times(1)).save(any(FarmGoal.class));
    }

    @Test
    @DisplayName("Deve ser idempotente ao vincular fazenda já associada")
    void deveSerIdempotenteAoVincularFazendaJaAssociada() {
        when(stateGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(farmRepository.findById(20L)).thenReturn(Optional.of(farm2));
        when(farmGoalRepository.existsByIdFarmAndIdGoal(20L, 1L)).thenReturn(true);

        assertDoesNotThrow(() -> stateGoalService.addFarmToStateGoal(1L, 20L, admPrincipal));
        verify(farmGoalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar 403 ao vincular fazenda quando usuário não tem acesso à meta primária")
    void deveLancar403AoVincularFazendaSemAcessoAMetaPrimaria() {
        Farm foreignPrimaryFarm = Farm.builder().id(10L).idEnterprise(999L).build();
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(50L).build();
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(stateGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(foreignPrimaryFarm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> stateGoalService.addFarmToStateGoal(1L, 20L, employeePrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("gerenciar esta meta estadual"));
    }

    @Test
    @DisplayName("Deve lançar 403 ao vincular fazenda quando usuário não tem acesso à fazenda alvo")
    void deveLancar403AoVincularFazendaSemAcessoAFazendaAlvo() {
        Farm foreignTargetFarm = Farm.builder().id(20L).idEnterprise(999L).build();
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(50L).build();
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(stateGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(farmRepository.findById(20L)).thenReturn(Optional.of(foreignTargetFarm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> stateGoalService.addFarmToStateGoal(1L, 20L, employeePrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("vincular fazenda a esta meta estadual"));
    }

    @Test
    @DisplayName("Deve desvincular fazenda de meta estadual com sucesso")
    void deveDesvincularFazendaDeMetaEstadual() {
        when(stateGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(farmRepository.findById(20L)).thenReturn(Optional.of(farm2));

        assertDoesNotThrow(() -> stateGoalService.removeFarmFromStateGoal(1L, 20L, admPrincipal));
        verify(farmGoalRepository, times(1)).deleteByIdFarmAndIdGoal(20L, 1L);
    }

    @Test
    @DisplayName("Deve lançar 400 Bad Request ao tentar desvincular a fazenda principal da meta estadual")
    void deveLancar400AoTentarDesvincularFazendaPrincipal() {
        when(stateGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> stateGoalService.removeFarmFromStateGoal(1L, 10L, admPrincipal));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Não é permitido desvincular a fazenda principal da meta estadual. Para remover a meta, utilize o endpoint de exclusão.", ex.getReason());
        verify(farmGoalRepository, never()).deleteByIdFarmAndIdGoal(any(), any());
    }

    @Test
    @DisplayName("Deve lançar 403 ao desvincular fazenda quando usuário não tem acesso à meta primária")
    void deveLancar403AoDesvincularFazendaSemAcessoAMetaPrimaria() {
        Farm foreignPrimaryFarm = Farm.builder().id(10L).idEnterprise(999L).build();
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(50L).build();
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(stateGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(foreignPrimaryFarm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> stateGoalService.removeFarmFromStateGoal(1L, 20L, employeePrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("gerenciar esta meta estadual"));
    }

    @Test
    @DisplayName("Deve lançar 403 ao desvincular fazenda quando usuário não tem acesso à fazenda alvo")
    void deveLancar403AoDesvincularFazendaSemAcessoAFazendaAlvo() {
        Farm foreignTargetFarm = Farm.builder().id(20L).idEnterprise(999L).build();
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(50L).build();
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(stateGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(farmRepository.findById(20L)).thenReturn(Optional.of(foreignTargetFarm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> stateGoalService.removeFarmFromStateGoal(1L, 20L, employeePrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("desvincular fazenda desta meta estadual"));
    }

    @Test
    @DisplayName("Deve listar fazendas vinculadas à meta estadual a partir da junção")
    void deveListarFazendasDaMetaEstadualComJuncao() {
        when(stateGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(farmGoalRepository.findByIdGoal(1L))
                .thenReturn(List.of(FarmGoal.builder().id(1L).idFarm(10L).idGoal(1L).build()));
        when(farmRepository.findAllById(List.of(10L))).thenReturn(List.of(farm));

        List<FarmResponseDTO> farms = stateGoalService.getFarmsByStateGoalId(1L, admPrincipal);

        assertNotNull(farms);
        assertEquals(1, farms.size());
        assertEquals("Fazenda Teste", farms.get(0).name());
    }

    @Test
    @DisplayName("Deve retornar fazenda primária quando não houver registros em farm_goals")
    void deveListarFazendaPrimariaQuandoSemJuncao() {
        when(stateGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(farmGoalRepository.findByIdGoal(1L)).thenReturn(List.of());

        List<FarmResponseDTO> farms = stateGoalService.getFarmsByStateGoalId(1L, admPrincipal);

        assertNotNull(farms);
        assertEquals(1, farms.size());
        assertEquals(10L, farms.get(0).id());
    }

    // ==========================================
    // REGION ASSOCIATIONS (regions_goals & state_goal_regions)
    // ==========================================

    @Test
    @DisplayName("Deve adicionar região à meta estadual (regions_goals)")
    void deveAdicionarRegiaoAMetaEstadual() {
        when(stateGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(regionGoalRepository.existsByRegionAndIdGoal("Sul", 1L)).thenReturn(false);
        when(regionGoalRepository.save(any(RegionGoal.class)))
                .thenReturn(RegionGoal.builder().id(200L).region("Sul").idGoal(1L).build());

        assertDoesNotThrow(() -> stateGoalService.addRegionToStateGoal(1L, new RegionGoalRequestDTO("Sul"), admPrincipal));
        verify(regionGoalRepository, times(1)).save(any(RegionGoal.class));
        verify(stateGoalRegionRepository, times(1)).save(any(StateGoalRegion.class));
    }

    @Test
    @DisplayName("Deve ser idempotente ao adicionar região já existente")
    void deveSerIdempotenteAoAdicionarRegiaoJaExistente() {
        when(stateGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(regionGoalRepository.existsByRegionAndIdGoal("Sul", 1L)).thenReturn(true);

        assertDoesNotThrow(() -> stateGoalService.addRegionToStateGoal(1L, new RegionGoalRequestDTO("Sul"), admPrincipal));
        verify(regionGoalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve remover região da meta estadual")
    void deveRemoverRegiaoDaMetaEstadual() {
        RegionGoal rg = RegionGoal.builder().id(200L).region("Sul").idGoal(1L).build();
        when(stateGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(regionGoalRepository.findByRegionAndIdGoal("Sul", 1L)).thenReturn(Optional.of(rg));

        assertDoesNotThrow(() -> stateGoalService.removeRegionFromStateGoal(1L, "Sul", admPrincipal));
        verify(stateGoalRegionRepository, times(1)).deleteByIdGoalAndIdRegion(1L, 200L);
        verify(regionGoalRepository, times(1)).delete(rg);
    }

    @Test
    @DisplayName("Deve listar regiões vinculadas à meta estadual")
    void deveListarRegioesDaMetaEstadual() {
        when(stateGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(regionGoalRepository.findByIdGoal(1L))
                .thenReturn(List.of(
                        RegionGoal.builder().id(1L).region("Sudeste").idGoal(1L).build(),
                        RegionGoal.builder().id(2L).region("Sul").idGoal(1L).build()
                ));

        List<String> regions = stateGoalService.getRegionsByStateGoalId(1L, admPrincipal);

        assertNotNull(regions);
        assertEquals(2, regions.size());
        assertTrue(regions.contains("Sudeste"));
        assertTrue(regions.contains("Sul"));
    }

    @Test
    @DisplayName("Deve listar fazendas vinculadas à meta com sucesso para FARM_OWNER de fazenda secundária")
    void deveListarFazendasDaMetaParaFarmOwnerDeFazendaSecundaria() {
        FarmOwner secondaryOwner = FarmOwner.builder().id(3L).idFarm(20L).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(secondaryOwner));
        when(stateGoalRepository.findById(1L)).thenReturn(Optional.of(goal)); // goal.idFarm = 10L
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(farmGoalRepository.existsByIdFarmAndIdGoal(20L, 1L)).thenReturn(true);
        when(farmGoalRepository.findByIdGoal(1L))
                .thenReturn(List.of(
                        FarmGoal.builder().id(1L).idFarm(10L).idGoal(1L).build(),
                        FarmGoal.builder().id(2L).idFarm(20L).idGoal(1L).build()
                ));
        when(farmRepository.findAllById(List.of(10L, 20L))).thenReturn(List.of(farm, farm2));

        List<FarmResponseDTO> farms = stateGoalService.getFarmsByStateGoalId(1L, ownerPrincipal);

        assertNotNull(farms);
        assertEquals(2, farms.size());
    }

    @Test
    @DisplayName("Deve listar regiões vinculadas à meta com sucesso para FARM_OWNER de fazenda secundária")
    void deveListarRegioesDaMetaParaFarmOwnerDeFazendaSecundaria() {
        FarmOwner secondaryOwner = FarmOwner.builder().id(3L).idFarm(20L).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(secondaryOwner));
        when(stateGoalRepository.findById(1L)).thenReturn(Optional.of(goal)); // goal.idFarm = 10L
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(farmGoalRepository.existsByIdFarmAndIdGoal(20L, 1L)).thenReturn(true);
        when(regionGoalRepository.findByIdGoal(1L))
                .thenReturn(List.of(RegionGoal.builder().id(1L).region("Sudeste").idGoal(1L).build()));

        List<String> regions = stateGoalService.getRegionsByStateGoalId(1L, ownerPrincipal);

        assertNotNull(regions);
        assertEquals(1, regions.size());
        assertEquals("Sudeste", regions.get(0));
    }

    @Test
    @DisplayName("Deve lançar 403 ao listar fazendas da meta para FARM_OWNER sem vínculo")
    void deveLancar403AoListarFazendasParaFarmOwnerSemVinculo() {
        FarmOwner unlinkedOwner = FarmOwner.builder().id(3L).idFarm(99L).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(unlinkedOwner));
        when(stateGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(farmGoalRepository.existsByIdFarmAndIdGoal(99L, 1L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> stateGoalService.getFarmsByStateGoalId(1L, ownerPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve listar metas para fazenda única utilizando a região customizada em regions_goals")
    void deveListarMetasParaFazendaUnicaComRegiaoCustomizada() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(farmGoalRepository.findByIdFarm(10L)).thenReturn(List.of());
        when(stateGoalRepository.findByIdFarm(10L)).thenReturn(List.of(goal));
        when(regionGoalRepository.findByIdGoal(1L))
                .thenReturn(List.of(RegionGoal.builder().id(10L).region("Nordeste").idGoal(1L).build()));

        List<StateGoalResponseDTO> result = stateGoalService.getStateGoalsForUser(10L, "Nordeste", admPrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Nordeste", result.get(0).region());
    }
}
