package com.ourosapp.springapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.dto.individualgoal.IndividualGoalRequestDTO;
import com.ourosapp.springapi.dto.individualgoal.IndividualGoalResponseDTO;
import com.ourosapp.springapi.dto.individualgoal.IndividualGoalUpdateDTO;
import com.ourosapp.springapi.security.KeycloakJwtAuthenticationConverter;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.IndividualGoalService;
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

/**
 * Testes de integração Web via MockMvc para o controlador {@link IndividualGoalController}.
 */
@WebMvcTest(IndividualGoalController.class)
@Import({SecurityConfig.class, KeycloakJwtAuthenticationConverter.class})
class IndividualGoalControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IndividualGoalService individualGoalService;

    @MockitoBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private final UserPrincipal mockPrincipal = new UserPrincipal(
            1L,
            "admin@ouros.com",
            "senha123",
            "ADM",
            List.of(new SimpleGrantedAuthority("ROLE_ADM"))
    );

    @Test
    @DisplayName("POST /individual-goals - Deve retornar 201 Created ao cadastrar meta individual com sucesso")
    void deveRetornar201AoCriarMetaIndividual() throws Exception {
        IndividualGoalRequestDTO requestDTO = new IndividualGoalRequestDTO(
                "Reduzir Consumo",
                "Redução noturna",
                "ENERGY_CONSUMPTION",
                "IN_PROGRESS",
                new BigDecimal("420.5000"),
                1L
        );
        IndividualGoalResponseDTO responseDTO = new IndividualGoalResponseDTO(
                10L,
                "Reduzir Consumo",
                "Redução noturna",
                "ENERGY_CONSUMPTION",
                "IN_PROGRESS",
                new BigDecimal("420.5000"),
                1L
        );

        when(individualGoalService.createIndividualGoal(any(IndividualGoalRequestDTO.class), any(UserPrincipal.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/individual-goals")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/individual-goals/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Reduzir Consumo"))
                .andExpect(jsonPath("$.target_value").value(420.5000))
                .andExpect(jsonPath("$.id_farm").value(1));
    }

    @Test
    @DisplayName("POST /individual-goals - Deve retornar 400 Bad Request quando payload for inválido")
    void deveRetornar400AoCriarMetaComPayloadInvalido() throws Exception {
        IndividualGoalRequestDTO invalidRequest = new IndividualGoalRequestDTO(
                "", // title em branco
                null,
                null, // type nulo
                null, // status nulo
                new BigDecimal("-10.00"), // targetValue negativo
                null
        );

        mockMvc.perform(post("/individual-goals")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /individual-goals - Deve retornar 401 Unauthorized quando requisição não autenticada")
    void deveRetornar401AoCriarMetaSemAutenticacao() throws Exception {
        mockMvc.perform(post("/individual-goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /individual-goals - Deve retornar 403 Forbidden quando usuário não tiver permissão")
    void deveRetornar403AoCriarMetaSemPermissao() throws Exception {
        IndividualGoalRequestDTO requestDTO = new IndividualGoalRequestDTO(
                "Meta", "Desc", "MORTALITY", "PENDING", new BigDecimal("2.5000"), 1L
        );

        when(individualGoalService.createIndividualGoal(any(IndividualGoalRequestDTO.class), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado"));

        mockMvc.perform(post("/individual-goals")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /individual-goals - Deve retornar 200 OK com a lista de metas individuais")
    void deveRetornar200AoListarMetas() throws Exception {
        IndividualGoalResponseDTO responseDTO = new IndividualGoalResponseDTO(
                10L, "Meta", "Desc", "MORTALITY", "PENDING", new BigDecimal("2.5000"), 1L
        );

        when(individualGoalService.getIndividualGoalsForUser(eq(null), any(UserPrincipal.class)))
                .thenReturn(List.of(responseDTO));

        mockMvc.perform(get("/individual-goals")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].title").value("Meta"));
    }

    @Test
    @DisplayName("GET /individual-goals - Deve retornar 200 OK ao filtrar por farm_id")
    void deveRetornar200AoListarMetasComFiltroFarmId() throws Exception {
        IndividualGoalResponseDTO responseDTO = new IndividualGoalResponseDTO(
                10L, "Meta", "Desc", "MORTALITY", "PENDING", new BigDecimal("2.5000"), 1L
        );

        when(individualGoalService.getIndividualGoalsForUser(eq(1L), any(UserPrincipal.class)))
                .thenReturn(List.of(responseDTO));

        mockMvc.perform(get("/individual-goals")
                        .param("farm_id", "1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].id_farm").value(1));
    }

    @Test
    @DisplayName("GET /individual-goals/{id} - Deve retornar 200 OK ao buscar meta por ID")
    void deveRetornar200AoBuscarMetaPorId() throws Exception {
        IndividualGoalResponseDTO responseDTO = new IndividualGoalResponseDTO(
                10L, "Meta", "Desc", "MORTALITY", "PENDING", new BigDecimal("2.5000"), 1L
        );

        when(individualGoalService.getIndividualGoalById(eq(10L), any(UserPrincipal.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(get("/individual-goals/10")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Meta"));
    }

    @Test
    @DisplayName("GET /individual-goals/{id} - Deve retornar 404 Not Found se meta não for encontrada")
    void deveRetornar404AoBuscarPorIdInexistente() throws Exception {
        when(individualGoalService.getIndividualGoalById(eq(99L), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Meta individual não encontrada"));

        mockMvc.perform(get("/individual-goals/99")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /individual-goals/{id} - Deve retornar 200 OK ao atualizar meta com sucesso")
    void deveRetornar200AoAtualizarMeta() throws Exception {
        IndividualGoalUpdateDTO updateDTO = new IndividualGoalUpdateDTO(
                "Novo Titulo", "Nova Descricao", "ACHIEVED", new BigDecimal("1.8000")
        );
        IndividualGoalResponseDTO responseDTO = new IndividualGoalResponseDTO(
                10L, "Novo Titulo", "Nova Descricao", "MORTALITY", "ACHIEVED", new BigDecimal("1.8000"), 1L
        );

        when(individualGoalService.updateIndividualGoal(eq(10L), any(IndividualGoalUpdateDTO.class), any(UserPrincipal.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(patch("/individual-goals/10")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Novo Titulo"))
                .andExpect(jsonPath("$.status").value("ACHIEVED"));
    }

    @Test
    @DisplayName("PATCH /individual-goals/{id} - Deve retornar 400 Bad Request quando valor alvo for negativo")
    void deveRetornar400AoAtualizarComValorAlvoNegativo() throws Exception {
        IndividualGoalUpdateDTO invalidUpdate = new IndividualGoalUpdateDTO(
                null, null, null, new BigDecimal("-5.00")
        );

        mockMvc.perform(patch("/individual-goals/10")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUpdate)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /individual-goals/{id} - Deve retornar 204 No Content ao remover meta")
    void deveRetornar204AoDeletarMeta() throws Exception {
        doNothing().when(individualGoalService).deleteIndividualGoal(eq(10L), any(UserPrincipal.class));

        mockMvc.perform(delete("/individual-goals/10")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /individual-goals/{id} - Deve retornar 403 Forbidden quando usuário não tiver permissão")
    void deveRetornar403AoDeletarMetaSemPermissao() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado"))
                .when(individualGoalService).deleteIndividualGoal(eq(10L), any(UserPrincipal.class));

        mockMvc.perform(delete("/individual-goals/10")
                        .with(user(mockPrincipal)))
                .andExpect(status().isForbidden());
    }
}
