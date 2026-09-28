package com.ourosapp.springapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.constants.RoleConstants;
import com.ourosapp.springapi.dto.plan.PlanRequestDTO;
import com.ourosapp.springapi.dto.plan.PlanResponseDTO;
import com.ourosapp.springapi.dto.plan.PlanUpdateDTO;
import com.ourosapp.springapi.security.KeycloakJwtAuthenticationConverter;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.PlanService;
import com.ourosapp.springapi.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração WebMvc para o {@link PlanController}.
 */
@WebMvcTest(PlanController.class)
@Import(SecurityConfig.class)
class PlanControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PlanService planService;

    @MockitoBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private final UserPrincipal adminPrincipal = new UserPrincipal(
            1L,
            "admin@agroouros.com.br",
            null,
            RoleConstants.ADM,
            List.of(new SimpleGrantedAuthority("ROLE_ADM"))
    );

    private final UserPrincipal employeePrincipal = new UserPrincipal(
            2L,
            "colab@agroouros.com.br",
            null,
            RoleConstants.COMPANY_EMPLOYEE,
            List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE"))
    );

    private final UserPrincipal farmOwnerPrincipal = new UserPrincipal(
            3L,
            "produtor@agroouros.com.br",
            null,
            RoleConstants.FARM_OWNER,
            List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER"))
    );

    @Test
    @DisplayName("POST /plans - Deve criar plano e retornar 201 Created com cabeçalho Location")
    void testCreatePlanSuccess() throws Exception {
        PlanRequestDTO request = new PlanRequestDTO(
                "Plano Safra Ouro",
                365,
                "Acesso completo à plataforma Ouros App",
                new BigDecimal("2990.00")
        );

        PlanResponseDTO response = new PlanResponseDTO(
                1L,
                "Plano Safra Ouro",
                365,
                "Acesso completo à plataforma Ouros App",
                new BigDecimal("2990.00")
        );

        when(planService.createPlan(any(PlanRequestDTO.class), eq(adminPrincipal))).thenReturn(response);

        mockMvc.perform(post("/plans")
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/plans/1")))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Plano Safra Ouro"))
                .andExpect(jsonPath("$.duration_days").value(365))
                .andExpect(jsonPath("$.description").value("Acesso completo à plataforma Ouros App"))
                .andExpect(jsonPath("$.price").value(2990.00));
    }

    @Test
    @DisplayName("POST /plans - Deve aceitar payload no formato camelCase (interoperabilidade)")
    void testCreatePlanCamelCasePayloadSuccess() throws Exception {
        String camelCaseJson = """
                {
                    "title": "Plano Anual",
                    "durationDays": 365,
                    "description": "Descrição do plano anual",
                    "price": 1990.00
                }
                """;

        PlanResponseDTO response = new PlanResponseDTO(
                2L,
                "Plano Anual",
                365,
                "Descrição do plano anual",
                new BigDecimal("1990.00")
        );

        when(planService.createPlan(any(PlanRequestDTO.class), eq(adminPrincipal))).thenReturn(response);

        mockMvc.perform(post("/plans")
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(camelCaseJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.duration_days").value(365));
    }

    @Test
    @DisplayName("POST /plans - Deve retornar 400 Bad Request quando payload for inválido")
    void testCreatePlanValidationErrors() throws Exception {
        // Título em branco
        String invalidTitle = """
                {
                    "title": "   ",
                    "duration_days": 30,
                    "description": "Desc",
                    "price": 100.00
                }
                """;

        mockMvc.perform(post("/plans")
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidTitle))
                .andExpect(status().isBadRequest());

        // Duração inválida (<= 0)
        String invalidDuration = """
                {
                    "title": "Plano Teste",
                    "duration_days": 0,
                    "description": "Desc",
                    "price": 100.00
                }
                """;

        mockMvc.perform(post("/plans")
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidDuration))
                .andExpect(status().isBadRequest());

        // Preço inválido (<= 0)
        String invalidPrice = """
                {
                    "title": "Plano Teste",
                    "duration_days": 30,
                    "description": "Desc",
                    "price": 0.00
                }
                """;

        mockMvc.perform(post("/plans")
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPrice))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /plans - Deve retornar 401 Unauthorized quando não autenticado")
    void testCreatePlanUnauthenticated() throws Exception {
        PlanRequestDTO request = new PlanRequestDTO(
                "Plano Safra Ouro",
                365,
                "Descrição",
                new BigDecimal("2990.00")
        );

        mockMvc.perform(post("/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /plans - Deve retornar 403 Forbidden quando perfil não for ADM")
    void testCreatePlanForbidden() throws Exception {
        PlanRequestDTO request = new PlanRequestDTO(
                "Plano Safra Ouro",
                365,
                "Descrição",
                new BigDecimal("2990.00")
        );

        when(planService.createPlan(any(PlanRequestDTO.class), eq(employeePrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado"));

        mockMvc.perform(post("/plans")
                        .with(user(employeePrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /plans - Deve retornar 409 Conflict quando título já estiver cadastrado")
    void testCreatePlanConflict() throws Exception {
        PlanRequestDTO request = new PlanRequestDTO(
                "Plano Duplicado",
                365,
                "Descrição",
                new BigDecimal("2990.00")
        );

        when(planService.createPlan(any(PlanRequestDTO.class), eq(adminPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um plano cadastrado"));

        mockMvc.perform(post("/plans")
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /plans - Deve retornar lista de planos com status 200 OK")
    void testGetAllPlansSuccess() throws Exception {
        PlanResponseDTO plan1 = new PlanResponseDTO(1L, "Plano 1", 30, "Desc 1", new BigDecimal("100.00"));
        PlanResponseDTO plan2 = new PlanResponseDTO(2L, "Plano 2", 60, "Desc 2", new BigDecimal("200.00"));

        when(planService.getAllPlans(eq(employeePrincipal))).thenReturn(List.of(plan1, plan2));

        mockMvc.perform(get("/plans")
                        .with(user(employeePrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Plano 1"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].title").value("Plano 2"));
    }

    @Test
    @DisplayName("GET /plans - Deve retornar 401 Unauthorized quando não autenticado")
    void testGetAllPlansUnauthenticated() throws Exception {
        mockMvc.perform(get("/plans"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /plans - Deve retornar 403 Forbidden quando usuário não tiver permissão")
    void testGetAllPlansForbidden() throws Exception {
        when(planService.getAllPlans(eq(farmOwnerPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado"));

        mockMvc.perform(get("/plans")
                        .with(user(farmOwnerPrincipal)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /plans/{id} - Deve retornar plano por ID com status 200 OK")
    void testGetPlanByIdSuccess() throws Exception {
        PlanResponseDTO plan = new PlanResponseDTO(1L, "Plano Safra Ouro", 365, "Desc", new BigDecimal("2990.00"));

        when(planService.getPlanById(eq(1L), eq(adminPrincipal))).thenReturn(plan);

        mockMvc.perform(get("/plans/1")
                        .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Plano Safra Ouro"));
    }

    @Test
    @DisplayName("GET /plans/{id} - Deve retornar 404 Not Found quando plano não existir")
    void testGetPlanByIdNotFound() throws Exception {
        when(planService.getPlanById(eq(99L), eq(adminPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano não encontrado"));

        mockMvc.perform(get("/plans/99")
                        .with(user(adminPrincipal)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /plans/{id} - Deve atualizar parcialmente o plano e retornar 200 OK")
    void testPatchPlanSuccess() throws Exception {
        PlanUpdateDTO request = new PlanUpdateDTO(
                "Plano Safra Platinum",
                null,
                null,
                new BigDecimal("3500.00")
        );

        PlanResponseDTO response = new PlanResponseDTO(
                1L,
                "Plano Safra Platinum",
                365,
                "Descrição antiga",
                new BigDecimal("3500.00")
        );

        when(planService.updatePlan(eq(1L), any(PlanUpdateDTO.class), eq(adminPrincipal))).thenReturn(response);

        mockMvc.perform(patch("/plans/1")
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Plano Safra Platinum"))
                .andExpect(jsonPath("$.price").value(3500.00));
    }

    @Test
    @DisplayName("PATCH /plans/{id} - Deve retornar 400 Bad Request se campos informados forem inválidos")
    void testPatchPlanInvalidFields() throws Exception {
        String invalidDuration = """
                {
                    "duration_days": -5
                }
                """;

        mockMvc.perform(patch("/plans/1")
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidDuration))
                .andExpect(status().isBadRequest());

        String invalidPrice = """
                {
                    "price": 0.00
                }
                """;

        mockMvc.perform(patch("/plans/1")
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPrice))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /plans/{id} - Deve atualizar plano e retornar 200 OK")
    void testPutPlanSuccess() throws Exception {
        PlanUpdateDTO request = new PlanUpdateDTO(
                "Plano Safra Diamante",
                730,
                "Descrição completa diamante",
                new BigDecimal("5990.00")
        );

        PlanResponseDTO response = new PlanResponseDTO(
                1L,
                "Plano Safra Diamante",
                730,
                "Descrição completa diamante",
                new BigDecimal("5990.00")
        );

        when(planService.updatePlan(eq(1L), any(PlanUpdateDTO.class), eq(adminPrincipal))).thenReturn(response);

        mockMvc.perform(put("/plans/1")
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Plano Safra Diamante"))
                .andExpect(jsonPath("$.duration_days").value(730))
                .andExpect(jsonPath("$.price").value(5990.00));
    }

    @Test
    @DisplayName("DELETE /plans/{id} - Deve remover plano e retornar 204 No Content")
    void testDeletePlanSuccess() throws Exception {
        doNothing().when(planService).deletePlan(eq(1L), eq(adminPrincipal));

        mockMvc.perform(delete("/plans/1")
                        .with(user(adminPrincipal)))
                .andExpect(status().isNoContent());

        verify(planService, times(1)).deletePlan(eq(1L), eq(adminPrincipal));
    }

    @Test
    @DisplayName("DELETE /plans/{id} - Deve retornar 404 Not Found quando plano não existir")
    void testDeletePlanNotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano não encontrado"))
                .when(planService).deletePlan(eq(99L), eq(adminPrincipal));

        mockMvc.perform(delete("/plans/99")
                        .with(user(adminPrincipal)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /plans/{id} - Deve retornar 409 Conflict quando houver vínculo relacional ativo")
    void testDeletePlanConflict() throws Exception {
        doThrow(new org.springframework.dao.DataIntegrityViolationException("Não é possível remover o plano"))
                .when(planService).deletePlan(eq(1L), eq(adminPrincipal));

        mockMvc.perform(delete("/plans/1")
                        .with(user(adminPrincipal)))
                .andExpect(status().isConflict());
    }
}
