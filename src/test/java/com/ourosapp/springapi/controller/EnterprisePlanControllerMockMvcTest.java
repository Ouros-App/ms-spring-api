package com.ourosapp.springapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.dto.enterpriseplan.EnterprisePlanRequestDTO;
import com.ourosapp.springapi.dto.enterpriseplan.EnterprisePlanResponseDTO;
import com.ourosapp.springapi.security.KeycloakJwtAuthenticationConverter;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.EnterprisePlanService;
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

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração Web via MockMvc para o controlador {@link EnterprisePlanController}.
 */
@WebMvcTest(EnterprisePlanController.class)
@Import(SecurityConfig.class)
class EnterprisePlanControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EnterprisePlanService enterprisePlanService;

    @MockitoBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private final UserPrincipal mockPrincipal = new UserPrincipal(
            1L,
            "admin@ouros.com",
            "password123",
            "ADM",
            List.of(new SimpleGrantedAuthority("ROLE_ADM"))
    );

    @Test
    @DisplayName("POST /enterprise-plans - Deve contratar plano e retornar 201 Created com cabeçalho Location")
    void testCreateEnterprisePlanSuccess() throws Exception {
        EnterprisePlanRequestDTO request = new EnterprisePlanRequestDTO(1L, 2L);
        EnterprisePlanResponseDTO response = new EnterprisePlanResponseDTO(10L, 1L, 2L, null, null);

        when(enterprisePlanService.createEnterprisePlan(any(EnterprisePlanRequestDTO.class), eq(mockPrincipal)))
                .thenReturn(response);

        mockMvc.perform(post("/enterprise-plans")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/enterprise-plans/10")))
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.id_enterprise").value(1L))
                .andExpect(jsonPath("$.id_plan").value(2L));
    }

    @Test
    @DisplayName("POST /enterprise-plans - Deve aceitar payload camelCase (interoperabilidade)")
    void testCreateEnterprisePlanCamelCasePayload() throws Exception {
        String camelCasePayload = """
                {
                    "idEnterprise": 1,
                    "idPlan": 2
                }
                """;
        EnterprisePlanResponseDTO response = new EnterprisePlanResponseDTO(10L, 1L, 2L, null, null);

        when(enterprisePlanService.createEnterprisePlan(any(EnterprisePlanRequestDTO.class), eq(mockPrincipal)))
                .thenReturn(response);

        mockMvc.perform(post("/enterprise-plans")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(camelCasePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    @DisplayName("POST /enterprise-plans - Deve retornar 400 Bad Request quando payload for inválido")
    void testCreateEnterprisePlanInvalidPayload() throws Exception {
        String invalidPayload = """
                {
                    "id_enterprise": -1,
                    "id_plan": null
                }
                """;

        mockMvc.perform(post("/enterprise-plans")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /enterprise-plans - Deve retornar 401 Unauthorized quando não autenticado")
    void testCreateEnterprisePlanUnauthorized() throws Exception {
        EnterprisePlanRequestDTO request = new EnterprisePlanRequestDTO(1L, 2L);

        mockMvc.perform(post("/enterprise-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /enterprise-plans - Deve retornar 403 Forbidden quando usuário sem permissão")
    void testCreateEnterprisePlanForbidden() throws Exception {
        EnterprisePlanRequestDTO request = new EnterprisePlanRequestDTO(1L, 2L);
        when(enterprisePlanService.createEnterprisePlan(any(EnterprisePlanRequestDTO.class), eq(mockPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado"));

        mockMvc.perform(post("/enterprise-plans")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /enterprise-plans - Deve retornar 404 Not Found quando empresa ou plano não existir")
    void testCreateEnterprisePlanNotFound() throws Exception {
        EnterprisePlanRequestDTO request = new EnterprisePlanRequestDTO(1L, 2L);
        when(enterprisePlanService.createEnterprisePlan(any(EnterprisePlanRequestDTO.class), eq(mockPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada"));

        mockMvc.perform(post("/enterprise-plans")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /enterprise-plans - Deve retornar 409 Conflict quando empresa já possuir o plano")
    void testCreateEnterprisePlanConflict() throws Exception {
        EnterprisePlanRequestDTO request = new EnterprisePlanRequestDTO(1L, 2L);
        when(enterprisePlanService.createEnterprisePlan(any(EnterprisePlanRequestDTO.class), eq(mockPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Plano já contratado"));

        mockMvc.perform(post("/enterprise-plans")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /enterprise-plans - Deve retornar lista de planos com status 200 OK")
    void testGetEnterprisePlansSuccess() throws Exception {
        EnterprisePlanResponseDTO ep = new EnterprisePlanResponseDTO(1L, 10L, 20L, null, null);
        when(enterprisePlanService.getEnterprisePlans(eq(10L), eq(20L), eq(mockPrincipal)))
                .thenReturn(List.of(ep));

        mockMvc.perform(get("/enterprise-plans")
                        .param("enterprise_id", "10")
                        .param("plan_id", "20")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].id_enterprise").value(10L))
                .andExpect(jsonPath("$[0].id_plan").value(20L));
    }

    @Test
    @DisplayName("GET /enterprise-plans - Deve retornar 401 Unauthorized quando não autenticado")
    void testGetEnterprisePlansUnauthorized() throws Exception {
        mockMvc.perform(get("/enterprise-plans"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /enterprise-plans/{id} - Deve retornar plano por ID com status 200 OK")
    void testGetEnterprisePlanByIdSuccess() throws Exception {
        EnterprisePlanResponseDTO ep = new EnterprisePlanResponseDTO(1L, 10L, 20L, null, null);
        when(enterprisePlanService.getEnterprisePlanById(eq(1L), eq(mockPrincipal))).thenReturn(ep);

        mockMvc.perform(get("/enterprise-plans/1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.id_enterprise").value(10L));
    }

    @Test
    @DisplayName("GET /enterprise-plans/{id} - Deve retornar 404 Not Found quando plano não existir")
    void testGetEnterprisePlanByIdNotFound() throws Exception {
        when(enterprisePlanService.getEnterprisePlanById(eq(99L), eq(mockPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano não encontrado"));

        mockMvc.perform(get("/enterprise-plans/99")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /enterprise-plans/{id} - Deve cancelar plano e retornar 204 No Content")
    void testDeleteEnterprisePlanSuccess() throws Exception {
        doNothing().when(enterprisePlanService).deleteEnterprisePlan(eq(1L), eq(mockPrincipal));

        mockMvc.perform(delete("/enterprise-plans/1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /enterprise-plans/{id} - Deve retornar 404 Not Found quando plano não existir")
    void testDeleteEnterprisePlanNotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano não encontrado"))
                .when(enterprisePlanService).deleteEnterprisePlan(eq(99L), eq(mockPrincipal));

        mockMvc.perform(delete("/enterprise-plans/99")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /enterprise-plans/{id} - Deve retornar 409 Conflict quando existirem pagamentos vinculados")
    void testDeleteEnterprisePlanConflict() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Existem pagamentos vinculados"))
                .when(enterprisePlanService).deleteEnterprisePlan(eq(1L), eq(mockPrincipal));

        mockMvc.perform(delete("/enterprise-plans/1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isConflict());
    }
}
