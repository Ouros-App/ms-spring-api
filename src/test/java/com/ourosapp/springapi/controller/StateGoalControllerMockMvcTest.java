package com.ourosapp.springapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.dto.farm.FarmResponseDTO;
import com.ourosapp.springapi.dto.stategoal.RegionGoalRequestDTO;
import com.ourosapp.springapi.dto.stategoal.StateGoalRequestDTO;
import com.ourosapp.springapi.dto.stategoal.StateGoalResponseDTO;
import com.ourosapp.springapi.dto.stategoal.StateGoalUpdateDTO;
import com.ourosapp.springapi.security.KeycloakJwtAuthenticationConverter;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.StateGoalService;
import com.ourosapp.springapi.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Testes de integração Web via MockMvc para o controlador {@link StateGoalController}.
 */
@WebMvcTest(StateGoalController.class)
@Import(SecurityConfig.class)
class StateGoalControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StateGoalService stateGoalService;

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
    @DisplayName("POST /state-goals - Deve retornar 201 Created ao cadastrar meta estadual")
    void deveRetornar201AoCriarMetaEstadual() throws Exception {
        StateGoalRequestDTO requestDTO = new StateGoalRequestDTO(
                "Meta Regional SP", "Meta estadual", "FEED_CONVERSION", "IN_PROGRESS",
                new BigDecimal("1.6500"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                1L, "Sudeste"
        );
        StateGoalResponseDTO responseDTO = new StateGoalResponseDTO(
                10L, "Meta Regional SP", "Meta estadual", "FEED_CONVERSION", "IN_PROGRESS",
                new BigDecimal("1.6500"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                1L, "Sudeste"
        );

        when(stateGoalService.createStateGoal(any(StateGoalRequestDTO.class), any(UserPrincipal.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/state-goals")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/state-goals/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Meta Regional SP"));
    }

    @Test
    @DisplayName("GET /state-goals - Deve retornar 200 OK com lista de metas")
    void deveRetornar200AoListarMetasEstaduais() throws Exception {
        StateGoalResponseDTO responseDTO = new StateGoalResponseDTO(
                10L, "Meta Regional SP", "Meta estadual", "FEED_CONVERSION", "IN_PROGRESS",
                new BigDecimal("1.6500"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                1L, "Sudeste"
        );

        when(stateGoalService.getStateGoalsForUser(eq(1L), eq("Sudeste"), any(UserPrincipal.class)))
                .thenReturn(List.of(responseDTO));

        mockMvc.perform(get("/state-goals")
                        .param("farm_id", "1")
                        .param("region", "Sudeste")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].title").value("Meta Regional SP"));
    }

    @Test
    @DisplayName("GET /state-goals/{id} - Deve retornar 200 OK com detalhes da meta")
    void deveRetornar200AoBuscarMetaPorId() throws Exception {
        StateGoalResponseDTO responseDTO = new StateGoalResponseDTO(
                10L, "Meta Regional SP", "Meta estadual", "FEED_CONVERSION", "IN_PROGRESS",
                new BigDecimal("1.6500"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                1L, "Sudeste"
        );

        when(stateGoalService.getStateGoalById(eq(10L), any(UserPrincipal.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(get("/state-goals/10")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.region").value("Sudeste"));
    }

    @Test
    @DisplayName("PATCH /state-goals/{id} - Deve retornar 200 OK com dados atualizados")
    void deveRetornar200AoAtualizarMetaEstadual() throws Exception {
        StateGoalUpdateDTO updateDTO = new StateGoalUpdateDTO("ACHIEVED", LocalDate.of(2026, 11, 30), new BigDecimal("1.5500"));
        StateGoalResponseDTO responseDTO = new StateGoalResponseDTO(
                10L, "Meta Regional SP", "Meta estadual", "FEED_CONVERSION", "ACHIEVED",
                new BigDecimal("1.5500"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 11, 30),
                1L, "Sudeste"
        );

        when(stateGoalService.updateStateGoal(eq(10L), any(StateGoalUpdateDTO.class), any(UserPrincipal.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(patch("/state-goals/10")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACHIEVED"));
    }

    @Test
    @DisplayName("DELETE /state-goals/{id} - Deve retornar 204 No Content ao excluir")
    void deveRetornar204AoExcluirMetaEstadual() throws Exception {
        doNothing().when(stateGoalService).deleteStateGoal(eq(10L), any(UserPrincipal.class));

        mockMvc.perform(delete("/state-goals/10")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /state-goals/{id}/farms/{farmId} - Deve retornar 204 ao vincular fazenda")
    void deveRetornar204AoVincularFazenda() throws Exception {
        doNothing().when(stateGoalService).addFarmToStateGoal(eq(10L), eq(20L), any(UserPrincipal.class));

        mockMvc.perform(post("/state-goals/10/farms/20")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /state-goals/{id}/farms/{farmId} - Deve retornar 204 ao desvincular fazenda")
    void deveRetornar204AoDesvincularFazenda() throws Exception {
        doNothing().when(stateGoalService).removeFarmFromStateGoal(eq(10L), eq(20L), any(UserPrincipal.class));

        mockMvc.perform(delete("/state-goals/10/farms/20")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /state-goals/{id}/farms - Deve retornar 200 OK com lista de fazendas")
    void deveRetornar200AoListarFazendas() throws Exception {
        FarmResponseDTO farmDTO = new FarmResponseDTO(
                20L, "Granja 2", new BigDecimal("100.00"), "Sudeste", 10000, "Setor 1", 1L, 5000, null, 2L
        );
        when(stateGoalService.getFarmsByStateGoalId(eq(10L), any(UserPrincipal.class)))
                .thenReturn(List.of(farmDTO));

        mockMvc.perform(get("/state-goals/10/farms")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(20))
                .andExpect(jsonPath("$[0].name").value("Granja 2"));
    }

    @Test
    @DisplayName("POST /state-goals/{id}/regions - Deve retornar 204 ao adicionar região")
    void deveRetornar204AoAdicionarRegiao() throws Exception {
        RegionGoalRequestDTO request = new RegionGoalRequestDTO("Sul");
        doNothing().when(stateGoalService).addRegionToStateGoal(eq(10L), any(RegionGoalRequestDTO.class), any(UserPrincipal.class));

        mockMvc.perform(post("/state-goals/10/regions")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /state-goals/{id}/regions/{region} - Deve retornar 204 ao remover região")
    void deveRetornar204AoRemoverRegiao() throws Exception {
        doNothing().when(stateGoalService).removeRegionFromStateGoal(eq(10L), eq("Sul"), any(UserPrincipal.class));

        mockMvc.perform(delete("/state-goals/10/regions/Sul")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /state-goals/{id}/regions - Deve retornar 200 OK com lista de regiões")
    void deveRetornar200AoListarRegioes() throws Exception {
        when(stateGoalService.getRegionsByStateGoalId(eq(10L), any(UserPrincipal.class)))
                .thenReturn(List.of("Sudeste", "Sul"));

        mockMvc.perform(get("/state-goals/10/regions")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Sudeste"))
                .andExpect(jsonPath("$[1]").value("Sul"));
    }
}
