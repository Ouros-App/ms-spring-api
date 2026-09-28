package com.ourosapp.springapi.service;

import com.ourosapp.springapi.constants.RoleConstants;
import com.ourosapp.springapi.dto.plan.PlanRequestDTO;
import com.ourosapp.springapi.dto.plan.PlanResponseDTO;
import com.ourosapp.springapi.dto.plan.PlanUpdateDTO;
import com.ourosapp.springapi.entity.Plan;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários da camada de serviço {@link PlanService}.
 */
@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;

    @InjectMocks
    private PlanService planService;

    private UserPrincipal adminPrincipal;
    private UserPrincipal employeePrincipal;
    private UserPrincipal farmOwnerPrincipal;

    private Plan samplePlan;
    private PlanRequestDTO validRequest;

    @BeforeEach
    void setUp() {
        adminPrincipal = new UserPrincipal(
                1L,
                "admin@agroouros.com.br",
                null,
                RoleConstants.ADM,
                List.of(new SimpleGrantedAuthority("ROLE_ADM"))
        );

        employeePrincipal = new UserPrincipal(
                2L,
                "funcionario@agroouros.com.br",
                null,
                RoleConstants.COMPANY_EMPLOYEE,
                List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE"))
        );

        farmOwnerPrincipal = new UserPrincipal(
                3L,
                "produtor@agroouros.com.br",
                null,
                RoleConstants.FARM_OWNER,
                List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER"))
        );

        samplePlan = Plan.builder()
                .id(1L)
                .title("Plano Safra Ouro")
                .durationDays(365)
                .description("Plano completo com todos os recursos")
                .price(new BigDecimal("2990.00"))
                .build();

        validRequest = new PlanRequestDTO(
                "Plano Safra Ouro",
                365,
                "Plano completo com todos os recursos",
                new BigDecimal("2990.00")
        );
    }

    @Test
    @DisplayName("createPlan - Deve cadastrar plano com sucesso quando usuário for ADM")
    void deveCadastrarPlanoComSucessoComoAdm() {
        when(planRepository.existsByTitleIgnoreCase("Plano Safra Ouro")).thenReturn(false);
        when(planRepository.save(any(Plan.class))).thenReturn(samplePlan);

        PlanResponseDTO response = planService.createPlan(validRequest, adminPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Plano Safra Ouro", response.title());
        assertEquals(365, response.durationDays());
        assertEquals("Plano completo com todos os recursos", response.description());
        assertEquals(new BigDecimal("2990.00"), response.price());

        verify(planRepository, times(1)).save(any(Plan.class));
    }

    @Test
    @DisplayName("createPlan - Deve lançar CONFLICT 409 quando já existir plano com o mesmo título")
    void deveLancarConflictAoCadastrarPlanoComTituloDuplicado() {
        when(planRepository.existsByTitleIgnoreCase("Plano Safra Ouro")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> planService.createPlan(validRequest, adminPrincipal));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Já existe um plano cadastrado"));
        verify(planRepository, never()).save(any(Plan.class));
    }

    @Test
    @DisplayName("createPlan - Deve propagar DataIntegrityViolationException ao salvar")
    void deveLancarConflictQuandoDataIntegrityViolationAoCadastrar() {
        when(planRepository.existsByTitleIgnoreCase("Plano Safra Ouro")).thenReturn(false);
        when(planRepository.save(any(Plan.class))).thenThrow(new DataIntegrityViolationException("Erro de constraint"));

        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class,
                () -> planService.createPlan(validRequest, adminPrincipal));

        assertNotNull(ex);
        assertTrue(ex.getMessage().contains("Erro de constraint"));
    }

    @Test
    @DisplayName("createPlan - Deve lançar FORBIDDEN 403 quando perfil for COMPANY_EMPLOYEE")
    void deveLancarForbiddenAoCadastrarPlanoComoCompanyEmployee() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> planService.createPlan(validRequest, employeePrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(planRepository, never()).save(any());
    }

    @Test
    @DisplayName("createPlan - Deve lançar FORBIDDEN 403 quando perfil for FARM_OWNER")
    void deveLancarForbiddenAoCadastrarPlanoComoFarmOwner() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> planService.createPlan(validRequest, farmOwnerPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(planRepository, never()).save(any());
    }

    @Test
    @DisplayName("createPlan - Deve lançar UNAUTHORIZED 401 quando usuário não estiver autenticado")
    void deveLancarUnauthorizedQuandoUsuarioNaoAutenticado() {
        ResponseStatusException ex1 = assertThrows(ResponseStatusException.class,
                () -> planService.createPlan(validRequest, null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex1.getStatusCode());

        UserPrincipal unauthenticated = new UserPrincipal(null, "anon@test.com", null, RoleConstants.ADM, List.of());
        ResponseStatusException ex2 = assertThrows(ResponseStatusException.class,
                () -> planService.createPlan(validRequest, unauthenticated));
        assertEquals(HttpStatus.UNAUTHORIZED, ex2.getStatusCode());
    }

    @Test
    @DisplayName("createPlan - Deve lançar NullPointerException quando payload de requisição for nulo")
    void deveLancarNpeQuandoRequestForNula() {
        assertThrows(NullPointerException.class, () -> planService.createPlan(null, adminPrincipal));
    }

    @Test
    @DisplayName("getAllPlans - Deve retornar lista de planos com sucesso para ADM e COMPANY_EMPLOYEE")
    void deveListarPlanosComSucessoParaAdmECompanyEmployee() {
        when(planRepository.findAll()).thenReturn(List.of(samplePlan));

        List<PlanResponseDTO> plansAdm = planService.getAllPlans(adminPrincipal);
        assertNotNull(plansAdm);
        assertEquals(1, plansAdm.size());
        assertEquals("Plano Safra Ouro", plansAdm.get(0).title());

        List<PlanResponseDTO> plansEmployee = planService.getAllPlans(employeePrincipal);
        assertNotNull(plansEmployee);
        assertEquals(1, plansEmployee.size());
    }

    @Test
    @DisplayName("getAllPlans - Deve lançar FORBIDDEN 403 para perfil FARM_OWNER")
    void deveLancarForbiddenAoListarPlanosComoFarmOwner() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> planService.getAllPlans(farmOwnerPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(planRepository, never()).findAll();
    }

    @Test
    @DisplayName("getPlanById - Deve buscar plano com sucesso para ID existente")
    void deveBuscarPlanoPorIdComSucesso() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(samplePlan));

        PlanResponseDTO response = planService.getPlanById(1L, adminPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Plano Safra Ouro", response.title());
    }

    @Test
    @DisplayName("getPlanById - Deve lançar NOT_FOUND 404 quando plano não existir ou ID for nulo")
    void deveLancarNotFoundAoBuscarPlanoInexistente() {
        when(planRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex1 = assertThrows(ResponseStatusException.class,
                () -> planService.getPlanById(99L, adminPrincipal));
        assertEquals(HttpStatus.NOT_FOUND, ex1.getStatusCode());

        ResponseStatusException ex2 = assertThrows(ResponseStatusException.class,
                () -> planService.getPlanById(null, adminPrincipal));
        assertEquals(HttpStatus.NOT_FOUND, ex2.getStatusCode());
    }

    @Test
    @DisplayName("getPlanById - Deve lançar FORBIDDEN 403 para perfil FARM_OWNER")
    void deveLancarForbiddenAoBuscarPlanoComoFarmOwner() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> planService.getPlanById(1L, farmOwnerPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(planRepository, never()).findById(any());
    }

    @Test
    @DisplayName("updatePlan - Deve atualizar campos do plano com sucesso como ADM")
    void deveAtualizarPlanoComSucessoComoAdm() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(samplePlan));
        when(planRepository.findByTitleIgnoreCase("Plano Safra Platinum")).thenReturn(Optional.empty());
        when(planRepository.save(any(Plan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlanUpdateDTO updateDTO = new PlanUpdateDTO(
                "Plano Safra Platinum",
                180,
                "Nova descrição platinum",
                new BigDecimal("4500.00")
        );

        PlanResponseDTO response = planService.updatePlan(1L, updateDTO, adminPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Plano Safra Platinum", response.title());
        assertEquals(180, response.durationDays());
        assertEquals("Nova descrição platinum", response.description());
        assertEquals(new BigDecimal("4500.00"), response.price());
    }

    @Test
    @DisplayName("updatePlan - Deve retornar plano inalterado se hasUpdates() for false")
    void deveRetornarMesmoPlanoQuandoNaoHouverAtualizacoes() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(samplePlan));

        PlanUpdateDTO emptyUpdate = new PlanUpdateDTO(null, null, null, null);

        PlanResponseDTO response = planService.updatePlan(1L, emptyUpdate, adminPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Plano Safra Ouro", response.title());
        verify(planRepository, never()).save(any());
    }

    @Test
    @DisplayName("updatePlan - Deve permitir manter o mesmo título no mesmo plano")
    void devePermitirAtualizacaoQuandoMesmoPlanoMantemOTitulo() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(samplePlan));
        when(planRepository.findByTitleIgnoreCase("Plano Safra Ouro")).thenReturn(Optional.of(samplePlan));
        when(planRepository.save(any(Plan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlanUpdateDTO updateDTO = new PlanUpdateDTO(
                "Plano Safra Ouro",
                730,
                null,
                null
        );

        PlanResponseDTO response = planService.updatePlan(1L, updateDTO, adminPrincipal);

        assertNotNull(response);
        assertEquals(730, response.durationDays());
        verify(planRepository, times(1)).save(any(Plan.class));
    }

    @Test
    @DisplayName("updatePlan - Deve lançar CONFLICT 409 quando tentar alterar para título de outro plano")
    void deveLancarConflictAoAtualizarPlanoParaTituloExistenteDeOutroPlano() {
        Plan anotherPlan = Plan.builder().id(2L).title("Plano Safra Platinum").build();

        when(planRepository.findById(1L)).thenReturn(Optional.of(samplePlan));
        when(planRepository.findByTitleIgnoreCase("Plano Safra Platinum")).thenReturn(Optional.of(anotherPlan));

        PlanUpdateDTO updateDTO = new PlanUpdateDTO("Plano Safra Platinum", null, null, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> planService.updatePlan(1L, updateDTO, adminPrincipal));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Já existe outro plano cadastrado"));
        verify(planRepository, never()).save(any());
    }

    @Test
    @DisplayName("updatePlan - Deve propagar DataIntegrityViolationException quando ocorrer ao salvar")
    void deveLancarConflictQuandoDataIntegrityViolationAoAtualizar() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(samplePlan));
        when(planRepository.save(any(Plan.class))).thenThrow(new DataIntegrityViolationException("Erro de integridade"));

        PlanUpdateDTO updateDTO = new PlanUpdateDTO(null, 180, null, null);

        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class,
                () -> planService.updatePlan(1L, updateDTO, adminPrincipal));

        assertNotNull(ex);
        assertTrue(ex.getMessage().contains("Erro de integridade"));
    }

    @Test
    @DisplayName("updatePlan - Deve lançar FORBIDDEN 403 quando não for ADM")
    void deveLancarForbiddenAoAtualizarPlanoComoCompanyEmployee() {
        PlanUpdateDTO updateDTO = new PlanUpdateDTO("Novo", null, null, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> planService.updatePlan(1L, updateDTO, employeePrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(planRepository, never()).save(any());
    }

    @Test
    @DisplayName("deletePlan - Deve remover plano com sucesso quando usuário for ADM")
    void deveExcluirPlanoComSucessoComoAdm() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(samplePlan));

        planService.deletePlan(1L, adminPrincipal);

        verify(planRepository, times(1)).delete(samplePlan);
        verify(planRepository, times(1)).flush();
    }

    @Test
    @DisplayName("deletePlan - Deve lançar NOT_FOUND 404 quando plano a remover não existir")
    void deveLancarNotFoundAoExcluirPlanoInexistente() {
        when(planRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> planService.deletePlan(99L, adminPrincipal));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(planRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deletePlan - Deve propagar DataIntegrityViolationException quando houver restrição de integridade")
    void deveLancarConflictAoExcluirPlanoComVinculoRelacional() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(samplePlan));
        doThrow(new DataIntegrityViolationException("FK constraint violation")).when(planRepository).flush();

        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class,
                () -> planService.deletePlan(1L, adminPrincipal));

        assertNotNull(ex);
        assertTrue(ex.getMessage().contains("FK constraint violation"));
    }

    @Test
    @DisplayName("deletePlan - Deve lançar FORBIDDEN 403 quando perfil não for ADM")
    void deveLancarForbiddenAoExcluirPlanoComoCompanyEmployee() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> planService.deletePlan(1L, employeePrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(planRepository, never()).delete(any());
    }
}
