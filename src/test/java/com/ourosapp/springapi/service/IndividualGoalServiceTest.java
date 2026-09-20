package com.ourosapp.springapi.service;

import static com.ourosapp.springapi.constants.RoleConstants.ADM;
import static com.ourosapp.springapi.constants.RoleConstants.COMPANY_EMPLOYEE;
import static com.ourosapp.springapi.constants.RoleConstants.FARM_OWNER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.ourosapp.springapi.dto.individualgoal.IndividualGoalRequestDTO;
import com.ourosapp.springapi.dto.individualgoal.IndividualGoalResponseDTO;
import com.ourosapp.springapi.dto.individualgoal.IndividualGoalUpdateDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.Farm;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.entity.IndividualGoal;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.repository.FarmRepository;
import com.ourosapp.springapi.repository.IndividualGoalRepository;
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
import java.util.List;
import java.util.Optional;

/**
 * Testes unitários para a camada de serviço {@link IndividualGoalService}.
 */
@ExtendWith(MockitoExtension.class)
class IndividualGoalServiceTest {

    @Mock
    private IndividualGoalRepository individualGoalRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private CompanyEmployeeRepository companyEmployeeRepository;

    @Mock
    private FarmOwnerRepository farmOwnerRepository;

    @InjectMocks
    private IndividualGoalService individualGoalService;

    private UserPrincipal admPrincipal;
    private UserPrincipal employeePrincipal;
    private UserPrincipal ownerPrincipal;
    private Farm farm;
    private IndividualGoal goal;
    private IndividualGoalRequestDTO requestDTO;

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

        goal = IndividualGoal.builder()
                .id(1L)
                .title("Reduzir Consumo")
                .description("Redução noturna")
                .type("ENERGY_CONSUMPTION")
                .status("IN_PROGRESS")
                .targetValue(new BigDecimal("420.5000"))
                .idFarm(10L)
                .build();

        requestDTO = new IndividualGoalRequestDTO(
                "Reduzir Consumo",
                "Redução noturna",
                "ENERGY_CONSUMPTION",
                "IN_PROGRESS",
                new BigDecimal("420.5000"),
                10L
        );
    }

    @Test
    @DisplayName("Deve criar meta individual com sucesso para usuário ADM")
    void deveCriarMetaIndividualComSucessoParaAdm() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(individualGoalRepository.save(any(IndividualGoal.class))).thenReturn(goal);

        IndividualGoalResponseDTO response = individualGoalService.createIndividualGoal(requestDTO, admPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Reduzir Consumo", response.title());
        assertEquals(new BigDecimal("420.5000"), response.targetValue());
        assertEquals(10L, response.idFarm());
        verify(individualGoalRepository, times(1)).save(any(IndividualGoal.class));
    }

    @Test
    @DisplayName("Deve criar meta individual com sucesso para Funcionário da mesma empresa")
    void deveCriarMetaIndividualComSucessoParaFuncionario() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(50L).build();
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(individualGoalRepository.save(any(IndividualGoal.class))).thenReturn(goal);

        IndividualGoalResponseDTO response = individualGoalService.createIndividualGoal(requestDTO, employeePrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        verify(individualGoalRepository, times(1)).save(any(IndividualGoal.class));
    }

    @Test
    @DisplayName("Deve criar meta individual com sucesso para Produtor Rural dono da fazenda")
    void deveCriarMetaIndividualComSucessoParaProdutorRural() {
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));
        when(individualGoalRepository.save(any(IndividualGoal.class))).thenReturn(goal);

        IndividualGoalResponseDTO response = individualGoalService.createIndividualGoal(requestDTO, ownerPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        verify(individualGoalRepository, times(1)).save(any(IndividualGoal.class));
    }

    @Test
    @DisplayName("Deve inferir ID da fazenda para Produtor Rural quando não informado no request")
    void deveInferirIdDaFazendaParaProdutorRuralQuandoNaoInformado() {
        IndividualGoalRequestDTO requestWithoutFarm = new IndividualGoalRequestDTO(
                "Reduzir Consumo",
                "Redução noturna",
                "ENERGY_CONSUMPTION",
                "IN_PROGRESS",
                new BigDecimal("420.5000"),
                null
        );
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(individualGoalRepository.save(any(IndividualGoal.class))).thenReturn(goal);

        IndividualGoalResponseDTO response = individualGoalService.createIndividualGoal(requestWithoutFarm, ownerPrincipal);

        assertNotNull(response);
        assertEquals(10L, response.idFarm());
    }

    @Test
    @DisplayName("Deve lançar 400 Bad Request ao tentar criar meta sem idFarm para ADM")
    void deveLancar400AoCriarMetaSemIdFarmParaAdm() {
        IndividualGoalRequestDTO requestWithoutFarm = new IndividualGoalRequestDTO(
                "Reduzir Consumo",
                "Redução noturna",
                "ENERGY_CONSUMPTION",
                "IN_PROGRESS",
                new BigDecimal("420.5000"),
                null
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> individualGoalService.createIndividualGoal(requestWithoutFarm, admPrincipal));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("O ID da fazenda é obrigatório"));
    }

    @Test
    @DisplayName("Deve lançar 400 Bad Request ao tentar criar meta se Produtor Rural não tiver fazenda vinculada")
    void deveLancar400AoCriarMetaSeProdutorNaoTiverFazendaVinculada() {
        IndividualGoalRequestDTO requestWithoutFarm = new IndividualGoalRequestDTO(
                "Reduzir Consumo",
                "Redução noturna",
                "ENERGY_CONSUMPTION",
                "IN_PROGRESS",
                new BigDecimal("420.5000"),
                null
        );
        FarmOwner ownerSemFazenda = FarmOwner.builder().id(3L).idFarm(null).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(ownerSemFazenda));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> individualGoalService.createIndividualGoal(requestWithoutFarm, ownerPrincipal));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("não possui fazenda vinculada"));
    }

    @Test
    @DisplayName("Deve lançar 401 Unauthorized se usuário não estiver autenticado")
    void deveLancar401AoCriarMetaSemAutenticacao() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> individualGoalService.createIndividualGoal(requestDTO, null));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 403 Forbidden para Funcionário de empresa diferente da fazenda")
    void deveLancar403ParaFuncionarioDeEmpresaDiferente() {
        CompanyEmployee employeeOutraEmpresa = CompanyEmployee.builder().id(2L).idEnterprise(99L).build();
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employeeOutraEmpresa));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> individualGoalService.createIndividualGoal(requestDTO, employeePrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 403 Forbidden para Produtor Rural de fazenda diferente")
    void deveLancar403ParaProdutorDeOutraFazenda() {
        FarmOwner ownerOutraFazenda = FarmOwner.builder().id(3L).idFarm(99L).build();
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(ownerOutraFazenda));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> individualGoalService.createIndividualGoal(requestDTO, ownerPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 404 Not Found quando a fazenda informada não existir")
    void deveLancar404QuandoFazendaNaoExistir() {
        when(farmRepository.findById(10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> individualGoalService.createIndividualGoal(requestDTO, admPrincipal));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 409 Conflict ao ocorrer DataIntegrityViolationException ao salvar")
    void deveLancar409QuandoHouverViolacaoDeIntegridade() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(individualGoalRepository.save(any(IndividualGoal.class)))
                .thenThrow(new DataIntegrityViolationException("Chave duplicada ou constraint violada"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> individualGoalService.createIndividualGoal(requestDTO, admPrincipal));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve listar todas as metas para ADM sem filtro")
    void deveListarTodasAsMetasParaAdm() {
        when(individualGoalRepository.findAll()).thenReturn(List.of(goal));

        List<IndividualGoalResponseDTO> result = individualGoalService.getIndividualGoalsForUser(null, admPrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Reduzir Consumo", result.get(0).title());
    }

    @Test
    @DisplayName("Deve listar metas da empresa para Funcionário sem filtro")
    void deveListarMetasParaFuncionario() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(50L).build();
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(farmRepository.findAllByIdEnterprise(50L)).thenReturn(List.of(farm));
        when(individualGoalRepository.findByIdFarmIn(List.of(10L))).thenReturn(List.of(goal));

        List<IndividualGoalResponseDTO> result = individualGoalService.getIndividualGoalsForUser(null, employeePrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Deve retornar lista vazia para Funcionário sem fazendas cadastradas na empresa")
    void deveRetornarListaVaziaParaFuncionarioSemFazendas() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(50L).build();
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(farmRepository.findAllByIdEnterprise(50L)).thenReturn(List.of());

        List<IndividualGoalResponseDTO> result = individualGoalService.getIndividualGoalsForUser(null, employeePrincipal);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Deve listar metas da fazenda para Produtor Rural sem filtro")
    void deveListarMetasParaProdutorRural() {
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));
        when(individualGoalRepository.findByIdFarm(10L)).thenReturn(List.of(goal));

        List<IndividualGoalResponseDTO> result = individualGoalService.getIndividualGoalsForUser(null, ownerPrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Deve retornar lista vazia para Produtor Rural sem fazenda vinculada")
    void deveRetornarListaVaziaParaProdutorRuralSemFazenda() {
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(null).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));

        List<IndividualGoalResponseDTO> result = individualGoalService.getIndividualGoalsForUser(null, ownerPrincipal);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Deve filtrar metas por farm_id quando informado e usuário tiver acesso")
    void deveFiltrarMetasPorFarmIdComSucesso() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(individualGoalRepository.findByIdFarm(10L)).thenReturn(List.of(goal));

        List<IndividualGoalResponseDTO> result = individualGoalService.getIndividualGoalsForUser(10L, admPrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Deve buscar meta individual por ID com sucesso")
    void deveBuscarMetaPorIdComSucesso() {
        when(individualGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));

        IndividualGoalResponseDTO response = individualGoalService.getIndividualGoalById(1L, admPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    @Test
    @DisplayName("Deve lançar 404 Not Found ao buscar meta individual inexistente por ID")
    void deveLancar404AoBuscarMetaInexistentePorId() {
        when(individualGoalRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> individualGoalService.getIndividualGoalById(99L, admPrincipal));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 403 Forbidden ao buscar meta por ID de fazenda de outra empresa")
    void deveLancar403AoBuscarMetaDeOutraEmpresaPorId() {
        CompanyEmployee employeeOutraEmpresa = CompanyEmployee.builder().id(2L).idEnterprise(99L).build();
        when(individualGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employeeOutraEmpresa));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> individualGoalService.getIndividualGoalById(1L, employeePrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 403 Forbidden ao atualizar meta de outra fazenda por Produtor Rural")
    void deveLancar403AoAtualizarMetaDeOutraFazendaPorProdutorRural() {
        FarmOwner ownerOutraFazenda = FarmOwner.builder().id(3L).idFarm(99L).build();
        IndividualGoalUpdateDTO updateDTO = new IndividualGoalUpdateDTO("Novo Titulo", null, null, null);
        when(individualGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(ownerOutraFazenda));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> individualGoalService.updateIndividualGoal(1L, updateDTO, ownerPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 403 Forbidden ao deletar meta de outra fazenda por Produtor Rural")
    void deveLancar403AoDeletarMetaDeOutraFazendaPorProdutorRural() {
        FarmOwner ownerOutraFazenda = FarmOwner.builder().id(3L).idFarm(99L).build();
        when(individualGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(ownerOutraFazenda));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> individualGoalService.deleteIndividualGoal(1L, ownerPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve atualizar meta individual parcialmente com sucesso")
    void deveAtualizarMetaIndividualComSucesso() {
        IndividualGoalUpdateDTO updateDTO = new IndividualGoalUpdateDTO(
                "Novo Titulo",
                "Nova Descricao",
                "ACHIEVED",
                new BigDecimal("400.0000")
        );

        when(individualGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(individualGoalRepository.save(any(IndividualGoal.class))).thenReturn(goal);

        IndividualGoalResponseDTO response = individualGoalService.updateIndividualGoal(1L, updateDTO, admPrincipal);

        assertNotNull(response);
        verify(individualGoalRepository, times(1)).save(goal);
    }

    @Test
    @DisplayName("Não deve salvar no repositório se o updateDTO não tiver campos para atualizar")
    void naoDeveSalvarSeUpdateDTONaoTiverCampos() {
        IndividualGoalUpdateDTO emptyUpdate = new IndividualGoalUpdateDTO(null, null, null, null);

        when(individualGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));

        IndividualGoalResponseDTO response = individualGoalService.updateIndividualGoal(1L, emptyUpdate, admPrincipal);

        assertNotNull(response);
        verify(individualGoalRepository, never()).save(any(IndividualGoal.class));
    }

    @Test
    @DisplayName("Deve remover meta individual com sucesso")
    void deveRemoverMetaIndividualComSucesso() {
        when(individualGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        doNothing().when(individualGoalRepository).delete(goal);

        assertDoesNotThrow(() -> individualGoalService.deleteIndividualGoal(1L, admPrincipal));
        verify(individualGoalRepository, times(1)).delete(goal);
    }

    @Test
    @DisplayName("Deve lançar 404 Not Found ao deletar meta inexistente")
    void deveLancar404AoDeletarMetaInexistente() {
        when(individualGoalRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> individualGoalService.deleteIndividualGoal(99L, admPrincipal));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
