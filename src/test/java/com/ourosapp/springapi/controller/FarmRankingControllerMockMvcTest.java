package com.ourosapp.springapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.dto.farm.FarmRankingResponseDTO;
import com.ourosapp.springapi.dto.farm.FarmScoreUpdateDTO;
import com.ourosapp.springapi.security.KeycloakJwtAuthenticationConverter;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.FarmRankingService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração Web via MockMvc para o controlador {@link FarmRankingController}.
 */
@WebMvcTest(FarmRankingController.class)
@Import(SecurityConfig.class)
class FarmRankingControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FarmRankingService farmRankingService;

    @MockitoBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private UserPrincipal createMockUser(String role) {
        return new UserPrincipal(
                1L,
                "usuario@teste.com",
                "Usuario Teste",
                role,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

    @Test
    @DisplayName("Deve retornar 200 OK com lista de granjas no ranking")
    void deveRetornarTopRankingsComSucesso() throws Exception {
        UserPrincipal principal = createMockUser("ADM");
        FarmRankingResponseDTO item = new FarmRankingResponseDTO(
                1L, 1L, 98.5, "Granja Sol Poente", "Sudeste", 50000, 48000
        );

        when(farmRankingService.getTopRankings(eq(1L), eq(10), any(UserPrincipal.class)))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/farms/rankings")
                        .with(user(principal))
                        .param("id_enterprise", "1")
                        .param("limit", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].farm_id").value(1))
                .andExpect(jsonPath("$[0].rank_position").value(1))
                .andExpect(jsonPath("$[0].score").value(98.5))
                .andExpect(jsonPath("$[0].farm_name").value("Granja Sol Poente"));
    }

    @Test
    @DisplayName("Deve retornar 200 OK com posição individual da granja")
    void deveRetornarPosicaoDaGranjaComSucesso() throws Exception {
        UserPrincipal principal = createMockUser("COMPANY_EMPLOYEE");
        FarmRankingResponseDTO item = new FarmRankingResponseDTO(
                2L, 3L, 85.0, "Granja Aurora", "Sul", 30000, 25000
        );

        when(farmRankingService.getFarmPosition(eq(2L), any(UserPrincipal.class)))
                .thenReturn(item);

        mockMvc.perform(get("/farms/2/ranking")
                        .with(user(principal))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.farm_id").value(2))
                .andExpect(jsonPath("$.rank_position").value(3))
                .andExpect(jsonPath("$.score").value(85.0))
                .andExpect(jsonPath("$.farm_name").value("Granja Aurora"));
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found quando a granja não existir")
    void deveRetornar404QuandoGranjaNaoExistir() throws Exception {
        UserPrincipal principal = createMockUser("COMPANY_EMPLOYEE");

        when(farmRankingService.getFarmPosition(eq(999L), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Fazenda não encontrada"));

        mockMvc.perform(get("/farms/999/ranking")
                        .with(user(principal))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar 200 OK ao atualizar pontuação de ranking de uma fazenda")
    void deveAtualizarScoreComSucesso() throws Exception {
        UserPrincipal principal = createMockUser("COMPANY_EMPLOYEE");
        FarmScoreUpdateDTO dto = new FarmScoreUpdateDTO(97.3);
        FarmRankingResponseDTO responseDTO = new FarmRankingResponseDTO(
                1L, 1L, 97.3, "Granja Boa Vista", "Sudeste", 40000, 39000
        );

        when(farmRankingService.updateFarmScore(eq(1L), any(FarmScoreUpdateDTO.class), any(UserPrincipal.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(put("/farms/1/ranking/score")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(97.3))
                .andExpect(jsonPath("$.farm_name").value("Granja Boa Vista"));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request ao informar score negativo")
    void deveRetornar400QuandoScoreInvalido() throws Exception {
        UserPrincipal principal = createMockUser("ADM");
        FarmScoreUpdateDTO dto = new FarmScoreUpdateDTO(-10.0);

        mockMvc.perform(put("/farms/1/ranking/score")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar 200 OK ao disparar sincronização de ranking")
    void deveSincronizarRankingComSucesso() throws Exception {
        UserPrincipal principal = createMockUser("ADM");

        when(farmRankingService.syncAllFarmsToRanking(eq(1L), any(UserPrincipal.class)))
                .thenReturn(5L);

        mockMvc.perform(post("/farms/rankings/sync")
                        .with(user(principal))
                        .param("id_enterprise", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.synced_farms").value(5))
                .andExpect(jsonPath("$.message").value("Sincronização de ranking concluída com sucesso"));
    }
}
