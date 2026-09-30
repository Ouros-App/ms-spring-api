package com.ourosapp.springapi.service;

import com.ourosapp.springapi.constants.RoleConstants;
import com.ourosapp.springapi.dto.enterpriseplan.EnterprisePlanRequestDTO;
import com.ourosapp.springapi.dto.enterpriseplan.EnterprisePlanResponseDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.EnterprisePlan;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.EnterprisePlanRepository;
import com.ourosapp.springapi.repository.EnterpriseRepository;
import com.ourosapp.springapi.repository.PlanRepository;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link EnterprisePlanService}.
 */
@ExtendWith(MockitoExtension.class)
class EnterprisePlanServiceTest {

    @Mock
    private EnterprisePlanRepository enterprisePlanRepository;

    @Mock
    private EnterpriseRepository enterpriseRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private CompanyEmployeeRepository companyEmployeeRepository;

    @InjectMocks
    private EnterprisePlanService enterprisePlanService;

    private UserPrincipal adminPrincipal;
    private UserPrincipal employeePrincipal;
    private UserPrincipal farmOwnerPrincipal;

    @BeforeEach
    void setUp() {
        adminPrincipal = new UserPrincipal(1L, "admin@ouros.com", "pass", RoleConstants.ADM,
                List.of(new SimpleGrantedAuthority("ROLE_ADM")));
        employeePrincipal = new UserPrincipal(2L, "employee@empresa.com", "pass", RoleConstants.COMPANY_EMPLOYEE,
                List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE")));
        farmOwnerPrincipal = new UserPrincipal(3L, "owner@fazenda.com", "pass", RoleConstants.FARM_OWNER,
                List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER")));
    }

    @Test
    @DisplayName("createEnterprisePlan - Deve contratar plano com sucesso como ADM")
    void deveContratarPlanoComSucessoComoAdmin() {
        EnterprisePlanRequestDTO request = new EnterprisePlanRequestDTO(10L, 20L);
        EnterprisePlan saved = EnterprisePlan.builder().id(1L).idEnterprise(10L).idPlan(20L).build();

        when(enterpriseRepository.existsById(10L)).thenReturn(true);
        when(planRepository.existsById(20L)).thenReturn(true);
        when(enterprisePlanRepository.existsByIdEnterpriseAndIdPlan(10L, 20L)).thenReturn(false);
        when(enterprisePlanRepository.save(any(EnterprisePlan.class))).thenReturn(saved);

        EnterprisePlanResponseDTO response = enterprisePlanService.createEnterprisePlan(request, adminPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(10L, response.idEnterprise());
        assertEquals(20L, response.idPlan());
        verify(enterprisePlanRepository).save(any(EnterprisePlan.class));
    }

    @Test
    @DisplayName("createEnterprisePlan - Deve contratar plano com sucesso como COMPANY_EMPLOYEE inferindo a empresa")
    void deveContratarPlanoComSucessoComoCompanyEmployeeInferindoEmpresa() {
        EnterprisePlanRequestDTO request = new EnterprisePlanRequestDTO(null, 20L);
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(10L).build();
        EnterprisePlan saved = EnterprisePlan.builder().id(1L).idEnterprise(10L).idPlan(20L).build();

        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(enterpriseRepository.existsById(10L)).thenReturn(true);
        when(planRepository.existsById(20L)).thenReturn(true);
        when(enterprisePlanRepository.existsByIdEnterpriseAndIdPlan(10L, 20L)).thenReturn(false);
        when(enterprisePlanRepository.save(any(EnterprisePlan.class))).thenReturn(saved);

        EnterprisePlanResponseDTO response = enterprisePlanService.createEnterprisePlan(request, employeePrincipal);

        assertNotNull(response);
        assertEquals(10L, response.idEnterprise());
        assertEquals(20L, response.idPlan());
    }

    @Test
    @DisplayName("createEnterprisePlan - Deve lançar 401 quando não autenticado")
    void deveLancarExcecaoQuandoNaoAutenticado() {
        EnterprisePlanRequestDTO request = new EnterprisePlanRequestDTO(10L, 20L);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> enterprisePlanService.createEnterprisePlan(request, null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    @DisplayName("createEnterprisePlan - Deve lançar 403 quando FARM_OWNER tentar contratar plano")
    void deveLancarExcecaoQuandoFarmOwnerTentarContratarPlano() {
        EnterprisePlanRequestDTO request = new EnterprisePlanRequestDTO(10L, 20L);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> enterprisePlanService.createEnterprisePlan(request, farmOwnerPrincipal));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("createEnterprisePlan - Deve lançar 403 quando colaborador tentar contratar para outra empresa")
    void deveLancarExcecaoQuandoColaboradorTentarContratarParaOutraEmpresa() {
        EnterprisePlanRequestDTO request = new EnterprisePlanRequestDTO(99L, 20L);
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(10L).build();

        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> enterprisePlanService.createEnterprisePlan(request, employeePrincipal));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("createEnterprisePlan - Deve lançar 400 quando ADM não informar idEnterprise")
    void deveLancarExcecaoQuandoAdminNaoInformarIdEnterprise() {
        EnterprisePlanRequestDTO request = new EnterprisePlanRequestDTO(null, 20L);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> enterprisePlanService.createEnterprisePlan(request, adminPrincipal));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("createEnterprisePlan - Deve lançar 404 quando empresa não existir")
    void deveLancarExcecaoQuandoEmpresaNaoExistir() {
        EnterprisePlanRequestDTO request = new EnterprisePlanRequestDTO(99L, 20L);
        when(enterpriseRepository.existsById(99L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> enterprisePlanService.createEnterprisePlan(request, adminPrincipal));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("createEnterprisePlan - Deve lançar 404 quando plano não existir")
    void deveLancarExcecaoQuandoPlanoNaoExistir() {
        EnterprisePlanRequestDTO request = new EnterprisePlanRequestDTO(10L, 99L);
        when(enterpriseRepository.existsById(10L)).thenReturn(true);
        when(planRepository.existsById(99L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> enterprisePlanService.createEnterprisePlan(request, adminPrincipal));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("createEnterprisePlan - Deve lançar 409 quando empresa já possuir o plano contratado")
    void deveLancarExcecaoQuandoPlanoJaContratado() {
        EnterprisePlanRequestDTO request = new EnterprisePlanRequestDTO(10L, 20L);
        when(enterpriseRepository.existsById(10L)).thenReturn(true);
        when(planRepository.existsById(20L)).thenReturn(true);
        when(enterprisePlanRepository.existsByIdEnterpriseAndIdPlan(10L, 20L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> enterprisePlanService.createEnterprisePlan(request, adminPrincipal));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    @DisplayName("createEnterprisePlan - Deve lançar exceção original de banco (DataIntegrityViolationException)")
    void deveLancarExcecaoQuandoErroIntegridadeNoSave() {
        EnterprisePlanRequestDTO request = new EnterprisePlanRequestDTO(10L, 20L);
        when(enterpriseRepository.existsById(10L)).thenReturn(true);
        when(planRepository.existsById(20L)).thenReturn(true);
        when(enterprisePlanRepository.existsByIdEnterpriseAndIdPlan(10L, 20L)).thenReturn(false);
        when(enterprisePlanRepository.save(any(EnterprisePlan.class)))
                .thenThrow(new DataIntegrityViolationException("Erro de constraint"));

        assertThrows(DataIntegrityViolationException.class,
                () -> enterprisePlanService.createEnterprisePlan(request, adminPrincipal));
    }

    @Test
    @DisplayName("getEnterprisePlans - Deve listar planos com filtros por empresa e plano como ADM")
    void deveListarPlanosComFiltrosComoAdmin() {
        EnterprisePlan ep = EnterprisePlan.builder().id(1L).idEnterprise(10L).idPlan(20L).build();
        when(enterprisePlanRepository.findByIdEnterpriseAndIdPlan(10L, 20L)).thenReturn(Optional.of(ep));

        List<EnterprisePlanResponseDTO> result = enterprisePlanService.getEnterprisePlans(10L, 20L, adminPrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
    }

    @Test
    @DisplayName("getEnterprisePlans - Deve listar todos os planos como ADM quando sem filtros")
    void deveListarTodosOsPlanosComoAdmin() {
        EnterprisePlan ep1 = EnterprisePlan.builder().id(1L).idEnterprise(10L).idPlan(20L).build();
        EnterprisePlan ep2 = EnterprisePlan.builder().id(2L).idEnterprise(11L).idPlan(21L).build();
        when(enterprisePlanRepository.findAll()).thenReturn(List.of(ep1, ep2));

        List<EnterprisePlanResponseDTO> result = enterprisePlanService.getEnterprisePlans(null, null, adminPrincipal);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("getEnterprisePlans - Deve listar apenas os planos da sua empresa como COMPANY_EMPLOYEE")
    void deveListarPlanosDaSuaEmpresaComoCompanyEmployee() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(10L).build();
        EnterprisePlan ep = EnterprisePlan.builder().id(1L).idEnterprise(10L).idPlan(20L).build();

        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(enterprisePlanRepository.findByIdEnterprise(10L)).thenReturn(List.of(ep));

        List<EnterprisePlanResponseDTO> result = enterprisePlanService.getEnterprisePlans(null, null, employeePrincipal);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).idEnterprise());
    }

    @Test
    @DisplayName("getEnterprisePlans - Deve lançar 403 quando COMPANY_EMPLOYEE tentar filtrar por outra empresa")
    void deveLancarExcecaoQuandoColaboradorFiltrarOutraEmpresa() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(10L).build();
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> enterprisePlanService.getEnterprisePlans(99L, null, employeePrincipal));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("getEnterprisePlans - Deve lançar 403 para perfil FARM_OWNER")
    void deveLancarExcecaoAoListarComPerfilInvalido() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> enterprisePlanService.getEnterprisePlans(null, null, farmOwnerPrincipal));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("getEnterprisePlanById - Deve buscar plano por ID com sucesso como ADM")
    void deveBuscarPlanoPorIdComoAdmin() {
        EnterprisePlan ep = EnterprisePlan.builder().id(1L).idEnterprise(10L).idPlan(20L).build();
        when(enterprisePlanRepository.findById(1L)).thenReturn(Optional.of(ep));

        EnterprisePlanResponseDTO response = enterprisePlanService.getEnterprisePlanById(1L, adminPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    @Test
    @DisplayName("getEnterprisePlanById - Deve buscar plano por ID com sucesso como COMPANY_EMPLOYEE da mesma empresa")
    void deveBuscarPlanoPorIdComoCompanyEmployee() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(10L).build();
        EnterprisePlan ep = EnterprisePlan.builder().id(1L).idEnterprise(10L).idPlan(20L).build();

        when(enterprisePlanRepository.findById(1L)).thenReturn(Optional.of(ep));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));

        EnterprisePlanResponseDTO response = enterprisePlanService.getEnterprisePlanById(1L, employeePrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    @Test
    @DisplayName("getEnterprisePlanById - Deve lançar 403 quando colaborador buscar plano de outra empresa")
    void deveLancarExcecaoQuandoColaboradorBuscarPlanoDeOutraEmpresa() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(10L).build();
        EnterprisePlan ep = EnterprisePlan.builder().id(1L).idEnterprise(99L).idPlan(20L).build();

        when(enterprisePlanRepository.findById(1L)).thenReturn(Optional.of(ep));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> enterprisePlanService.getEnterprisePlanById(1L, employeePrincipal));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("getEnterprisePlanById - Deve lançar 404 quando plano não for encontrado")
    void deveLancarExcecaoQuandoPlanoNaoEncontrado() {
        when(enterprisePlanRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> enterprisePlanService.getEnterprisePlanById(99L, adminPrincipal));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("deleteEnterprisePlan - Deve excluir plano com sucesso como ADM")
    void deveExcluirPlanoComoAdmin() {
        EnterprisePlan ep = EnterprisePlan.builder().id(1L).idEnterprise(10L).idPlan(20L).build();
        when(enterprisePlanRepository.findById(1L)).thenReturn(Optional.of(ep));

        assertDoesNotThrow(() -> enterprisePlanService.deleteEnterprisePlan(1L, adminPrincipal));
        verify(enterprisePlanRepository).delete(ep);
    }

    @Test
    @DisplayName("deleteEnterprisePlan - Deve excluir plano com sucesso como COMPANY_EMPLOYEE da mesma empresa")
    void deveExcluirPlanoComoCompanyEmployee() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(10L).build();
        EnterprisePlan ep = EnterprisePlan.builder().id(1L).idEnterprise(10L).idPlan(20L).build();

        when(enterprisePlanRepository.findById(1L)).thenReturn(Optional.of(ep));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));

        assertDoesNotThrow(() -> enterprisePlanService.deleteEnterprisePlan(1L, employeePrincipal));
        verify(enterprisePlanRepository).delete(ep);
    }

    @Test
    @DisplayName("deleteEnterprisePlan - Deve lançar 403 quando colaborador tentar excluir plano de outra empresa")
    void deveLancarExcecaoQuandoColaboradorTentarExcluirPlanoDeOutraEmpresa() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(10L).build();
        EnterprisePlan ep = EnterprisePlan.builder().id(1L).idEnterprise(99L).idPlan(20L).build();

        when(enterprisePlanRepository.findById(1L)).thenReturn(Optional.of(ep));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> enterprisePlanService.deleteEnterprisePlan(1L, employeePrincipal));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("deleteEnterprisePlan - Deve lançar exceção original de banco quando houver violação de integridade (pagamentos vinculados)")
    void deveLancarExcecaoQuandoExcluirPlanoComPagamentosVinculados() {
        EnterprisePlan ep = EnterprisePlan.builder().id(1L).idEnterprise(10L).idPlan(20L).build();
        when(enterprisePlanRepository.findById(1L)).thenReturn(Optional.of(ep));
        doThrow(new DataIntegrityViolationException("FK constraint")).when(enterprisePlanRepository).flush();

        assertThrows(DataIntegrityViolationException.class,
                () -> enterprisePlanService.deleteEnterprisePlan(1L, adminPrincipal));
    }
}
